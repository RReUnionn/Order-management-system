package com.vupl.orderservice.event.payload;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedPayload {
    private String eventId;
    private String orderId;
    private String orderCode;
    private String userId;
    private String status;
    private String reason;
}
