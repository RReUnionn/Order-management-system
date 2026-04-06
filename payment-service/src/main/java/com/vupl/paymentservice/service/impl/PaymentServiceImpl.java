package com.vupl.paymentservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vupl.paymentservice.dto.request.*;
import com.vupl.paymentservice.dto.response.*;
import com.vupl.paymentservice.entity.*;
import com.vupl.paymentservice.entity.Payment.*;
import com.vupl.paymentservice.event.payload.*;
import com.vupl.paymentservice.exception.AppException;
import com.vupl.paymentservice.repository.*;
import com.vupl.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final RefundRepository refundRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.payment-completed}")
    private String paymentCompletedTopic;
    @Value("${kafka.topics.payment-failed}")
    private String paymentFailedTopic;
    @Value("${payment.expiry-minutes:15}")
    private int expiryMinutes;
    @Value("${payment.mock-gateway-enabled:true}")
    private boolean mockGatewayEnabled;

    // ── Saga: Consume order.confirmed → tạo payment record ───

    @Override
    public void handleOrderConfirmed(OrderConfirmedPayload payload) {
        if (isProcessed(payload.getEventId())) return;

        if (paymentRepository.existsByOrderId(payload.getOrderId())) {
            log.warn("Payment already exists for orderId={}", payload.getOrderId());
            markProcessed(payload.getEventId(), "order.confirmed");
            return;
        }

        Payment payment = Payment.builder()
                .orderId(payload.getOrderId())
                .userId(payload.getUserId())
                .amount(payload.getTotalAmount())
                .method(PaymentMethod.VNPAY) // default, user chọn sau
                .status(PaymentStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build();

        Payment saved = paymentRepository.save(payment);

        // Log event
        savePaymentEvent(saved.getId(), "CREATED", null, null, null);

        markProcessed(payload.getEventId(), "order.confirmed");
        log.info("Payment record created for orderId={} paymentId={}", payload.getOrderId(), saved.getId());
    }

    // ── Customer: khởi tạo thanh toán (chọn method + lấy URL)

    @Override
    public PaymentResponse initiatePayment(String orderId, String userId, InitiatePaymentRequest request) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy payment cho đơn hàng này"));

        if (!payment.getUserId().equals(userId))
            throw AppException.forbidden("Không có quyền thanh toán đơn hàng này");

        if (payment.getStatus() != PaymentStatus.PENDING)
            throw AppException.badRequest("Đơn hàng đã được thanh toán hoặc đã hủy");

        if (payment.getExpiredAt() != null && payment.getExpiredAt().isBefore(LocalDateTime.now()))
            throw AppException.badRequest("Link thanh toán đã hết hạn");

        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatus.PROCESSING);

        // Tạo gateway URL (mock hoặc thật)
        String gatewayUrl = buildGatewayUrl(payment, request);
        payment.setGatewayUrl(gatewayUrl);

        Payment saved = paymentRepository.save(payment);
        savePaymentEvent(saved.getId(), "GATEWAY_REDIRECT", null,
                "Redirecting to " + request.getMethod().name(), null);

        log.info("Payment initiated for orderId={} method={}", orderId, request.getMethod());
        return PaymentResponse.from(saved);
    }

    // ── Gateway Webhook callback ───────────────────────────────

    @Override
    public void handleGatewayCallback(MockGatewayCallbackRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> AppException.notFound("Payment không tồn tại cho orderId=" + request.getOrderId()));

        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.FAILED) {
            log.warn("Payment {} already finalized, ignoring callback", payment.getId());
            return;
        }

        if (request.isSuccess()) {
            // ✅ Thanh toán thành công
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setGatewayRef(request.getGatewayRef());
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            savePaymentEvent(payment.getId(), "COMPLETED", "00", "Thanh toán thành công",
                    toJson(request));

            // Publish payment.completed → Order Service sẽ update PAID
            saveOutbox(paymentCompletedTopic, payment.getId(),
                    PaymentCompletedPayload.builder()
                            .eventId(UUID.randomUUID().toString())
                            .paymentId(payment.getId())
                            .orderId(payment.getOrderId())
                            .userId(payment.getUserId())
                            .amount(payment.getAmount())
                            .method(payment.getMethod().name())
                            .gatewayRef(request.getGatewayRef())
                            .build());

            log.info("Payment COMPLETED for orderId={} gatewayRef={}", payment.getOrderId(), request.getGatewayRef());

        } else {
            // ❌ Thanh toán thất bại
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            String reason = request.getFailReason() != null ? request.getFailReason() : "Thanh toán thất bại";
            savePaymentEvent(payment.getId(), "FAILED", "99", reason, toJson(request));

            // Publish payment.failed → Order Service + Inventory Service sẽ rollback
            saveOutbox(paymentFailedTopic, payment.getId(),
                    PaymentFailedPayload.builder()
                            .eventId(UUID.randomUUID().toString())
                            .paymentId(payment.getId())
                            .orderId(payment.getOrderId())
                            .userId(payment.getUserId())
                            .reason(reason)
                            .build());

            log.info("Payment FAILED for orderId={} reason={}", payment.getOrderId(), reason);
        }
    }

    // ── Refund ────────────────────────────────────────────────

    @Override
    public RefundResponse requestRefund(String paymentId, String userId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> AppException.notFound("Payment không tồn tại"));

        if (!payment.getUserId().equals(userId))
            throw AppException.forbidden("Không có quyền hoàn tiền payment này");

        if (payment.getStatus() != PaymentStatus.COMPLETED)
            throw AppException.badRequest("Chỉ có thể hoàn tiền khi payment đã hoàn thành");

        if (request.getAmount().compareTo(payment.getAmount()) > 0)
            throw AppException.badRequest("Số tiền hoàn không được vượt quá số tiền thanh toán");

        Refund refund = Refund.builder()
                .paymentId(paymentId)
                .amount(request.getAmount())
                .reason(request.getReason())
                .requestedBy(userId)
                .build();

        Refund saved = refundRepository.save(refund);

        // Cập nhật trạng thái payment
        payment.setStatus(request.getAmount().compareTo(payment.getAmount()) == 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);

        savePaymentEvent(paymentId, "REFUND_REQUESTED", null,
                "Refund requested: " + request.getAmount(), null);

        return RefundResponse.from(saved);
    }

    // ── Query operations ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(String orderId, String userId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> AppException.notFound("Payment không tồn tại cho đơn hàng này"));
        if (userId != null && !payment.getUserId().equals(userId))
            throw AppException.forbidden("Không có quyền xem payment này");
        return PaymentResponse.from(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentEventResponse> getPaymentEvents(String paymentId, String userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> AppException.notFound("Payment không tồn tại"));
        if (userId != null && !payment.getUserId().equals(userId))
            throw AppException.forbidden("Không có quyền xem payment events");
        return paymentEventRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId)
                .stream().map(PaymentEventResponse::from).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefunds(String paymentId, String userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> AppException.notFound("Payment không tồn tại"));
        if (userId != null && !payment.getUserId().equals(userId))
            throw AppException.forbidden("Không có quyền xem refund");
        return refundRepository.findByPaymentId(paymentId)
                .stream().map(RefundResponse::from).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByStatus(String status) {
        PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
        return paymentRepository.findByStatus(paymentStatus)
                .stream().map(PaymentResponse::from).collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────

    private String buildGatewayUrl(Payment payment, InitiatePaymentRequest request) {
        if (mockGatewayEnabled) {
            // Mock URL — trong thực tế gọi SDK của VNPay/Momo
            return String.format("http://localhost:8085/api/payments/mock-gateway?orderId=%s&amount=%s&method=%s",
                    payment.getOrderId(), payment.getAmount(), payment.getMethod().name());
        }
        // TODO: Tích hợp VNPay/Momo SDK thật
        return "https://payment-gateway.example.com/pay?ref=" + UUID.randomUUID();
    }

    private void savePaymentEvent(String paymentId, String eventType,
                                  String code, String message, String payload) {
        paymentEventRepository.save(PaymentEvent.builder()
                .paymentId(paymentId).eventType(eventType)
                .gatewayCode(code).gatewayMessage(message).payload(payload).build());
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
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId).eventType(eventType).build());
    }

    private void saveOutbox(String topic, String aggregateId, Object payload) {
        try {
            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateType("PAYMENT").aggregateId(aggregateId)
                    .eventType(topic).payload(objectMapper.writeValueAsString(payload)).build());
        } catch (Exception e) {
            log.error("Failed to save outbox [{}] for {}: {}", topic, aggregateId, e.getMessage());
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
