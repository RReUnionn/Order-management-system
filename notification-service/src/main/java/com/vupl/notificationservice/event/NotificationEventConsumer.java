package com.vupl.notificationservice.event;

import com.vupl.notificationservice.event.payload.*;
import com.vupl.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    // ── Order events ──────────────────────────────────────────

    @KafkaListener(topics = "${kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderCreated(@Payload OrderEventPayload p, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("[{}] orderId={}", topic, p.getOrderId());
        safeHandle(() -> notificationService.handleOrderEvent("order.created", p));
    }

    @KafkaListener(topics = "${kafka.topics.order-confirmed}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderConfirmed(@Payload OrderEventPayload p, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("[{}] orderId={}", topic, p.getOrderId());
        safeHandle(() -> notificationService.handleOrderEvent("order.confirmed", p));
    }

    @KafkaListener(topics = "${kafka.topics.order-cancelled}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderCancelled(@Payload OrderEventPayload p, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("[{}] orderId={}", topic, p.getOrderId());
        safeHandle(() -> notificationService.handleOrderEvent("order.cancelled", p));
    }

    @KafkaListener(topics = "${kafka.topics.order-paid}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderPaid(@Payload OrderEventPayload p, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("[{}] orderId={}", topic, p.getOrderId());
        safeHandle(() -> notificationService.handleOrderEvent("order.paid", p));
    }

    @KafkaListener(topics = "${kafka.topics.order-shipping}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderShipping(@Payload OrderEventPayload p, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("[{}] orderId={}", topic, p.getOrderId());
        safeHandle(() -> notificationService.handleOrderEvent("order.shipping", p));
    }

    // ── Payment events ────────────────────────────────────────

    @KafkaListener(topics = "${kafka.topics.payment-completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentCompleted(@Payload PaymentEventPayload p, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("[{}] orderId={}", topic, p.getOrderId());
        safeHandle(() -> notificationService.handlePaymentEvent("payment.completed", p));
    }

    @KafkaListener(topics = "${kafka.topics.payment-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentFailed(@Payload PaymentEventPayload p, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("[{}] orderId={}", topic, p.getOrderId());
        safeHandle(() -> notificationService.handlePaymentEvent("payment.failed", p));
    }

    private void safeHandle(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("Error handling notification event: {}", e.getMessage(), e);
        }
    }
}
