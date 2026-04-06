package com.vupl.orderservice.event;

import com.vupl.orderservice.event.payload.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Component;
import com.vupl.orderservice.service.*;

@Component @RequiredArgsConstructor @Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "${kafka.topics.inventory-reserved}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onInventoryReserved(
            @Payload InventoryReservedPayload payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received [{}] offset={} orderId={}", topic, offset, payload.getOrderId());
        try { orderService.handleInventoryReserved(payload); }
        catch (Exception e) { log.error("Error handling inventory.reserved: {}", e.getMessage(), e); }
    }

    @KafkaListener(topics = "${kafka.topics.inventory-reserve-failed}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onInventoryReserveFailed(
            @Payload InventoryReserveFailedPayload payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received [{}] offset={} orderId={}", topic, offset, payload.getOrderId());
        try { orderService.handleInventoryReserveFailed(payload); }
        catch (Exception e) { log.error("Error handling inventory.reserve.failed: {}", e.getMessage(), e); }
    }

    @KafkaListener(topics = "${kafka.topics.payment-completed}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentCompleted(
            @Payload PaymentCompletedPayload payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received [{}] offset={} orderId={}", topic, offset, payload.getOrderId());
        try { orderService.handlePaymentCompleted(payload); }
        catch (Exception e) { log.error("Error handling payment.completed: {}", e.getMessage(), e); }
    }

    @KafkaListener(topics = "${kafka.topics.payment-failed}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentFailed(
            @Payload PaymentFailedPayload payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received [{}] offset={} orderId={}", topic, offset, payload.getOrderId());
        try { orderService.handlePaymentFailed(payload); }
        catch (Exception e) { log.error("Error handling payment.failed: {}", e.getMessage(), e); }
    }
}
