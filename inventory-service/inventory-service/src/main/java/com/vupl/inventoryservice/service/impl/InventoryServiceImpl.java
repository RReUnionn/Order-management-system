package com.vupl.inventoryservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vupl.inventoryservice.dto.request.*;
import com.vupl.inventoryservice.dto.response.*;
import com.vupl.inventoryservice.entity.*;
import com.vupl.inventoryservice.event.payload.*;
import com.vupl.inventoryservice.exception.AppException;
import com.vupl.inventoryservice.repository.*;
import com.vupl.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.inventory-reserved}")
    private String inventoryReservedTopic;
    @Value("${kafka.topics.inventory-reserve-failed}")
    private String inventoryReserveFailedTopic;
    @Value("${kafka.topics.inventory-released}")
    private String inventoryReleasedTopic;
    @Value("${kafka.topics.inventory-confirmed}")
    private String inventoryConfirmedTopic;

    // ── Admin operations ──────────────────────────────────────

    @Override
    public InventoryResponse importStock(ImportStockRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> AppException.notFound("Kho không tồn tại"));

        Inventory inv = inventoryRepository
                .findByProductIdAndWarehouseId(request.getProductId(), request.getWarehouseId())
                .orElseGet(() -> Inventory.builder()
                        .productId(request.getProductId()).warehouse(warehouse).build());

        int oldQty = inv.getQuantity();
        inv.setQuantity(oldQty + request.getQuantity());
        Inventory saved = inventoryRepository.save(inv);

        writeLog(saved, request.getQuantity(), saved.getQuantity(),
                InventoryLog.LogReason.IMPORT, null, request.getNote());

        return InventoryResponse.from(saved);
    }

    @Override
    public InventoryResponse adjustStock(String inventoryId, AdjustStockRequest request) {
        Inventory inv = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> AppException.notFound("Inventory không tồn tại"));

        int newQty = inv.getQuantity() + request.getDelta();
        if (newQty < 0) throw AppException.badRequest("Số lượng sau điều chỉnh không thể âm");

        inv.setQuantity(newQty);
        Inventory saved = inventoryRepository.save(inv);
        writeLog(saved, request.getDelta(), newQty,
                InventoryLog.LogReason.ADJUSTMENT, null, request.getNote());
        return InventoryResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getByProductId(String productId) {
        return inventoryRepository.findByProductId(productId)
                .stream().map(InventoryResponse::from).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getByProductAndWarehouse(String productId, String warehouseId) {
        return InventoryResponse.from(inventoryRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> AppException.notFound("Inventory không tồn tại")));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryLogResponse> getLogs(String productId, Pageable pageable) {
        return inventoryLogRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(InventoryLogResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockItems() {
        return inventoryRepository.findLowStockItems()
                .stream().map(InventoryResponse::from).collect(Collectors.toList());
    }

    // ── Saga: Step 1 — Reserve stock khi order được tạo ──────

    @Override
    @Transactional
    public void handleOrderCreated(OrderCreatedPayload payload) {
        // Idempotent check
        if (isAlreadyProcessed(payload.getEventId())) {
            log.warn("Event {} already processed, skipping", payload.getEventId());
            return;
        }

        log.info("Processing order.created for orderId={}", payload.getOrderId());

        try {
            List<InventoryReservedPayload.ReservedItem> reservedItems = new ArrayList<>();

            for (OrderCreatedPayload.OrderItem item : payload.getItems()) {
                // Pessimistic lock để tránh race condition
                List<Inventory> inventories = inventoryRepository
                        .findByProductIdWithLock(item.getProductId());

                if (inventories.isEmpty()) {
                    publishReserveFailed(payload.getOrderId(), item.getProductId(),
                            "Sản phẩm " + item.getProductId() + " không có trong kho");
                    markProcessed(payload.getEventId(), "order.created");
                    return;
                }

                // Tìm inventory có đủ hàng
                Inventory inv = inventories.stream()
                        .filter(i -> i.getAvailableQty() >= item.getQuantity())
                        .findFirst().orElse(null);

                if (inv == null) {
                    publishReserveFailed(payload.getOrderId(), item.getProductId(),
                            "Sản phẩm " + item.getProductId() + " không đủ tồn kho");
                    markProcessed(payload.getEventId(), "order.created");
                    return;
                }

                // Reserve
                inv.setReservedQty(inv.getReservedQty() + item.getQuantity());
                inventoryRepository.save(inv);
                writeLog(inv, -item.getQuantity(), inv.getAvailableQty(),
                        InventoryLog.LogReason.RESERVE, payload.getOrderId(), null);

                reservedItems.add(InventoryReservedPayload.ReservedItem.builder()
                        .productId(item.getProductId()).quantity(item.getQuantity()).build());
            }

            // Tất cả items reserve thành công → publish inventory.reserved
            publishOutbox(inventoryReservedTopic, payload.getOrderId(),
                    InventoryReservedPayload.builder()
                            .eventId(UUID.randomUUID().toString())
                            .orderId(payload.getOrderId())
                            .success(true)
                            .items(reservedItems)
                            .build());

            markProcessed(payload.getEventId(), "order.created");
            log.info("Reserved stock successfully for orderId={}", payload.getOrderId());

        } catch (Exception e) {
            log.error("Error reserving stock for orderId={}: {}", payload.getOrderId(), e.getMessage());
            publishReserveFailed(payload.getOrderId(), null, "Lỗi hệ thống khi reserve tồn kho");
            markProcessed(payload.getEventId(), "order.created");
        }
    }

    // ── Saga: Compensation — Release stock khi payment thất bại

    @Override
    @Transactional
    public void handlePaymentFailed(PaymentFailedPayload payload) {
        if (isAlreadyProcessed(payload.getEventId())) {
            log.warn("Event {} already processed, skipping", payload.getEventId());
            return;
        }

        log.info("Processing payment.failed for orderId={}, releasing stock", payload.getOrderId());

        // Tìm tất cả log RESERVE theo orderId để biết cần release bao nhiêu
        List<InventoryLog> reserveLogs = inventoryLogRepository
                .findByReferenceIdOrderByCreatedAtAsc(payload.getOrderId())
                .stream()
                .filter(l -> l.getReason() == InventoryLog.LogReason.RESERVE)
                .collect(Collectors.toList());

        for (InventoryLog reserveLog : reserveLogs) {
            inventoryRepository.findById(reserveLog.getInventoryId()).ifPresent(inv -> {
                int releaseQty = Math.abs(reserveLog.getDelta());
                inv.setReservedQty(Math.max(0, inv.getReservedQty() - releaseQty));
                inventoryRepository.save(inv);
                writeLog(inv, releaseQty, inv.getAvailableQty(),
                        InventoryLog.LogReason.RELEASE, payload.getOrderId(), "Payment failed");
            });
        }

        publishOutbox(inventoryReleasedTopic, payload.getOrderId(),
                InventoryReleasedPayload.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderId(payload.getOrderId())
                        .build());

        markProcessed(payload.getEventId(), "payment.failed");
        log.info("Released stock for orderId={}", payload.getOrderId());
    }

    // ── Helpers ───────────────────────────────────────────────

    private boolean isAlreadyProcessed(String eventId) {
        return eventId != null && processedEventRepository.existsByEventId(eventId);
    }

    private void markProcessed(String eventId, String eventType) {
        if (eventId != null && !processedEventRepository.existsByEventId(eventId)) {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId).eventType(eventType).build());
        }
    }

    private void writeLog(Inventory inv, int delta, int qtyAfter,
                          InventoryLog.LogReason reason, String referenceId, String note) {
        inventoryLogRepository.save(InventoryLog.builder()
                .inventoryId(inv.getId()).productId(inv.getProductId())
                .delta(delta).qtyAfter(qtyAfter).reason(reason)
                .referenceId(referenceId).note(note).build());
    }

    private void publishReserveFailed(String orderId, String productId, String reason) {
        publishOutbox(inventoryReserveFailedTopic, orderId,
                InventoryReserveFailedPayload.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderId(orderId).productId(productId).reason(reason).build());
    }

    private void publishOutbox(String topic, String aggregateId, Object payload) {
        try {
            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateType("INVENTORY").aggregateId(aggregateId)
                    .eventType(topic).payload(objectMapper.writeValueAsString(payload)).build());
        } catch (Exception e) {
            log.error("Failed to save outbox event [{}] for {}: {}", topic, aggregateId, e.getMessage());
        }
    }
}
