package com.vupl.orderservice.event.payload;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedPayload {
    private String eventId;
    private String orderId;
    private String reason;
}
