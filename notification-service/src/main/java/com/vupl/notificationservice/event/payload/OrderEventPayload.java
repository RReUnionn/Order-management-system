package com.vupl.notificationservice.event.payload;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventPayload {
    private String eventId;
    private String orderId;
    private String orderCode;
    private String userId;
    private String status;
    private String reason;
    private BigDecimal totalAmount;
}
