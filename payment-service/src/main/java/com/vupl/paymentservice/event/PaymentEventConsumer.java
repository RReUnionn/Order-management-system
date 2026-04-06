package com.vupl.paymentservice.event;

import com.vupl.paymentservice.event.payload.OrderConfirmedPayload;
import com.vupl.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Slf4j
public class PaymentEventConsumer {

    private final PaymentService paymentService;

    /**
     * Lắng nghe order.confirmed → tạo payment record (Saga Step 2)
     */
    @KafkaListener(
            topics = "${kafka.topics.order-confirmed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onOrderConfirmed(
            @Payload OrderConfirmedPayload payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received [{}] offset={} orderId={}", topic, offset, payload.getOrderId());
        try {
            paymentService.handleOrderConfirmed(payload);
        } catch (Exception e) {
            log.error("Error handling order.confirmed for orderId={}: {}", payload.getOrderId(), e.getMessage(), e);
        }
    }
}
