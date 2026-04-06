package com.vupl.paymentservice.event.payload;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedPayload {
    private String eventId;
    private String paymentId;
    private String orderId;
    private String userId;
    private String reason;
}
