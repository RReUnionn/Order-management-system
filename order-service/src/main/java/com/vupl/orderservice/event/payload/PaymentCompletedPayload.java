package com.vupl.orderservice.event.payload;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedPayload {
    private String eventId;
    private String orderId;
    private String paymentId;
    private String gatewayRef;
}
