package com.vupl.inventoryservice.event;
import com.vupl.inventoryservice.event.payload.*;
import com.vupl.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final InventoryService inventoryService;

    /**
     * Lắng nghe order.created → reserve stock (Saga Step 1)
     */
    @KafkaListener(
            topics = "${kafka.topics.order-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(
            @Payload OrderCreatedPayload payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received [{}] offset={} orderId={}", topic, offset, payload.getOrderId());
        try {
            inventoryService.handleOrderCreated(payload);
        } catch (Exception e) {
            log.error("Error handling order.created for orderId={}: {}", payload.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Lắng nghe payment.failed → release stock (Saga Compensation)
     */
    @KafkaListener(
            topics = "${kafka.topics.payment-failed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentFailed(
            @Payload PaymentFailedPayload payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received [{}] offset={} orderId={}", topic, offset, payload.getOrderId());
        try {
            inventoryService.handlePaymentFailed(payload);
        } catch (Exception e) {
            log.error("Error handling payment.failed for orderId={}: {}", payload.getOrderId(), e.getMessage(), e);
        }
    }
}