package com.vupl.paymentservice.event.payload;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedPayload {
    private String eventId;
    private String paymentId;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String method;
    private String gatewayRef;
}
