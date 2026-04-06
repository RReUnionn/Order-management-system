package com.vupl.orderservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vupl.orderservice.dto.request.*;
import com.vupl.orderservice.dto.response.*;
import com.vupl.orderservice.entity.*;
import com.vupl.orderservice.entity.Order.OrderStatus;
import com.vupl.orderservice.event.payload.*;
import com.vupl.orderservice.exception.AppException;
import com.vupl.orderservice.repository.*;
import com.vupl.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;
    @Value("${kafka.topics.order-confirmed}")
    private String orderConfirmedTopic;
    @Value("${kafka.topics.order-cancelled}")
    private String orderCancelledTopic;
    @Value("${kafka.topics.order-paid}")
    private String orderPaidTopic;

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    // ── Create Order (Saga Start) ─────────────────────────────

    @Override
    public OrderResponse createOrder(String userId, CreateOrderRequest req) {
        String orderCode = generateOrderCode();

        // Build items + calculate pricing
        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderCode(orderCode).userId(userId)
                .shipRecipient(req.getShipRecipient()).shipPhone(req.getShipPhone())
                .shipProvince(req.getShipProvince()).shipDistrict(req.getShipDistrict())
                .shipWard(req.getShipWard()).shipStreet(req.getShipStreet())
                .note(req.getNote()).sagaStep("ORDER_CREATED")
                .build();

        for (CreateOrderRequest.OrderItemRequest itemReq : req.getItems()) {
            BigDecimal itemSubtotal = itemReq.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            items.add(OrderItem.builder()
                    .order(order).productId(itemReq.getProductId())
                    .productName(itemReq.getProductName()).productSku(itemReq.getProductSku())
                    .productImageUrl(itemReq.getProductImageUrl())
                    .unitPrice(itemReq.getUnitPrice()).quantity(itemReq.getQuantity())
                    .subtotal(itemSubtotal).build());
        }

        BigDecimal shippingFee = calculateShippingFee(subtotal);
        BigDecimal total = subtotal.add(shippingFee);

        order.setItems(items);
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(total);
        order.setStatusHistory(new ArrayList<>());
        order.getStatusHistory().add(buildHistory(order, null, OrderStatus.PENDING, "Tạo đơn hàng", "SYSTEM"));

        Order saved = orderRepository.save(order);

        // Publish order.created via Outbox
        List<OrderCreatedPayload.OrderItemPayload> itemPayloads = items.stream()
                .map(i -> OrderCreatedPayload.OrderItemPayload.builder()
                        .productId(i.getProductId()).productName(i.getProductName())
                        .quantity(i.getQuantity()).unitPrice(i.getUnitPrice()).build())
                .collect(Collectors.toList());

        saveOutbox(orderCreatedTopic, saved.getId(),
                OrderCreatedPayload.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderId(saved.getId()).orderCode(saved.getOrderCode())
                        .userId(userId).totalAmount(total).items(itemPayloads).build());

        log.info("Order created: {} ({})", saved.getOrderCode(), saved.getId());
        return OrderResponse.from(saved);
    }

    // ── Saga Step 2: Inventory reserved → CONFIRMED ───────────

    @Override
    public void handleInventoryReserved(InventoryReservedPayload payload) {
        if (isProcessed(payload.getEventId())) return;

        Order order = orderRepository.findById(payload.getOrderId())
                .orElseThrow(() -> AppException.notFound("Order không tồn tại: " + payload.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING) {
            markProcessed(payload.getEventId(), "inventory.reserved");
            return;
        }

        order.setSagaStep("INVENTORY_RESERVED");
        changeStatus(order, OrderStatus.CONFIRMED, "Tồn kho đã được giữ", "SYSTEM");
        orderRepository.save(order);

        saveOutbox(orderConfirmedTopic, order.getId(),
                OrderStatusChangedPayload.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderId(order.getId()).orderCode(order.getOrderCode())
                        .userId(order.getUserId()).status("CONFIRMED").build());

        markProcessed(payload.getEventId(), "inventory.reserved");
        log.info("Order {} CONFIRMED after inventory reserved", order.getOrderCode());
    }

    // ── Saga Compensation: Inventory reserve failed → CANCELLED

    @Override
    public void handleInventoryReserveFailed(InventoryReserveFailedPayload payload) {
        if (isProcessed(payload.getEventId())) return;

        orderRepository.findById(payload.getOrderId()).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setSagaStep("INVENTORY_RESERVE_FAILED");
                order.setCancelReason(payload.getReason());
                changeStatus(order, OrderStatus.CANCELLED, payload.getReason(), "SYSTEM");
                orderRepository.save(order);

                saveOutbox(orderCancelledTopic, order.getId(),
                        OrderStatusChangedPayload.builder()
                                .eventId(UUID.randomUUID().toString())
                                .orderId(order.getId()).orderCode(order.getOrderCode())
                                .userId(order.getUserId()).status("CANCELLED").reason(payload.getReason()).build());

                log.info("Order {} CANCELLED — inventory reserve failed: {}", order.getOrderCode(), payload.getReason());
            }
        });
        markProcessed(payload.getEventId(), "inventory.reserve.failed");
    }

    // ── Saga Step 3: Payment completed → PAID ────────────────

    @Override
    public void handlePaymentCompleted(PaymentCompletedPayload payload) {
        if (isProcessed(payload.getEventId())) return;

        orderRepository.findById(payload.getOrderId()).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.CONFIRMED) {
                order.setSagaStep("PAYMENT_COMPLETED");
                changeStatus(order, OrderStatus.PAID, "Thanh toán thành công", "SYSTEM");
                orderRepository.save(order);

                saveOutbox(orderPaidTopic, order.getId(),
                        OrderStatusChangedPayload.builder()
                                .eventId(UUID.randomUUID().toString())
                                .orderId(order.getId()).orderCode(order.getOrderCode())
                                .userId(order.getUserId()).status("PAID").build());

                log.info("Order {} PAID — gatewayRef: {}", order.getOrderCode(), payload.getGatewayRef());
            }
        });
        markProcessed(payload.getEventId(), "payment.completed");
    }

    // ── Saga Compensation: Payment failed → CANCELLED ─────────

    @Override
    public void handlePaymentFailed(PaymentFailedPayload payload) {
        if (isProcessed(payload.getEventId())) return;

        orderRepository.findById(payload.getOrderId()).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.CONFIRMED) {
                order.setSagaStep("PAYMENT_FAILED");
                order.setCancelReason(payload.getReason());
                changeStatus(order, OrderStatus.CANCELLED, payload.getReason(), "SYSTEM");
                orderRepository.save(order);

                saveOutbox(orderCancelledTopic, order.getId(),
                        OrderStatusChangedPayload.builder()
                                .eventId(UUID.randomUUID().toString())
                                .orderId(order.getId()).orderCode(order.getOrderCode())
                                .userId(order.getUserId()).status("CANCELLED").reason(payload.getReason()).build());

                log.info("Order {} CANCELLED — payment failed: {}", order.getOrderCode(), payload.getReason());
            }
        });
        markProcessed(payload.getEventId(), "payment.failed");
    }

    // ── Query operations ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Đơn hàng không tồn tại"));
        if (userId != null && !order.getUserId().equals(userId))
            throw AppException.forbidden("Không có quyền xem đơn hàng này");
        return OrderResponse.from(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String orderCode, String userId) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> AppException.notFound("Đơn hàng không tồn tại"));
        if (userId != null && !order.getUserId().equals(userId))
            throw AppException.forbidden("Không có quyền xem đơn hàng này");
        return OrderResponse.from(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(String userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        OrderStatus orderStatus = parseStatus(status);
        Page<Order> orders = orderRepository.findByUserIdAndStatus(userId, orderStatus, pageable);
        return PageResponse.from(orders.map(OrderResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = status != null
                ? orderRepository.findByStatusOrderByCreatedAtDesc(parseStatus(status), pageable)
                : orderRepository.findAll(pageable);
        return PageResponse.from(orders.map(OrderResponse::from));
    }

    @Override
    public void cancelOrder(String orderId, String userId, CancelOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Đơn hàng không tồn tại"));

        if (!order.getUserId().equals(userId))
            throw AppException.forbidden("Không có quyền hủy đơn hàng này");

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED)
            throw AppException.badRequest("Không thể hủy đơn ở trạng thái " + order.getStatus());

        order.setCancelReason(request.getReason());
        changeStatus(order, OrderStatus.CANCELLED, request.getReason(), userId);
        orderRepository.save(order);

        saveOutbox(orderCancelledTopic, order.getId(),
                OrderStatusChangedPayload.builder()
                        .eventId(UUID.randomUUID().toString())
                        .orderId(order.getId()).orderCode(order.getOrderCode())
                        .userId(order.getUserId()).status("CANCELLED").reason(request.getReason()).build());
    }

    @Override
    public OrderResponse updateStatus(String orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Đơn hàng không tồn tại"));
        OrderStatus newStatus = OrderStatus.valueOf(status);
        changeStatus(order, newStatus, "Admin cập nhật trạng thái", "ADMIN");
        return OrderResponse.from(orderRepository.save(order));
    }

    // ── Helpers ───────────────────────────────────────────────

    private String generateOrderCode() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "ORD-" + date + "-" + String.format("%04d", SEQ.getAndIncrement());
    }

    private BigDecimal calculateShippingFee(BigDecimal subtotal) {
        return subtotal.compareTo(BigDecimal.valueOf(500_000)) >= 0
                ? BigDecimal.ZERO : BigDecimal.valueOf(30_000);
    }

    private void changeStatus(Order order, OrderStatus newStatus, String reason, String changedBy) {
        String fromStatus = order.getStatus() != null ? order.getStatus().name() : null;
        order.setStatus(newStatus);
        if (order.getStatusHistory() == null) order.setStatusHistory(new ArrayList<>());
        order.getStatusHistory().add(buildHistory(order, fromStatus, newStatus, reason, changedBy));
    }

    private OrderStatusHistory buildHistory(Order order, String from, OrderStatus to, String reason, String by) {
        return OrderStatusHistory.builder()
                .order(order).fromStatus(from).toStatus(to.name()).reason(reason).changedBy(by).build();
    }

    private boolean isProcessed(String eventId) {
        if (eventId == null) return false;
        if (processedEventRepository.existsByEventId(eventId)) {
            log.warn("Event {} already processed, skipping", eventId);
            return true;
        }
        return false;
    }

    private void markProcessed(String eventId, String eventType) {
        if (eventId != null && !processedEventRepository.existsByEventId(eventId))
            processedEventRepository.save(ProcessedEvent.builder().eventId(eventId).eventType(eventType).build());
    }

    private void saveOutbox(String topic, String aggregateId, Object payload) {
        try {
            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateType("ORDER").aggregateId(aggregateId)
                    .eventType(topic).payload(objectMapper.writeValueAsString(payload)).build());
        } catch (Exception e) {
            log.error("Failed to save outbox [{}] for {}: {}", topic, aggregateId, e.getMessage());
        }
    }

    private OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("Status không hợp lệ: " + status);
        }
    }
}
