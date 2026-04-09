package com.vupl.notificationservice.event.payload;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventPayload {
    private String eventId;
    private String paymentId;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String method;
    private String reason;
}
