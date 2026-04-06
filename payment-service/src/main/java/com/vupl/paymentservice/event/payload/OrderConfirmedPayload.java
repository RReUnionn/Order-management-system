package com.vupl.paymentservice.event.payload;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedPayload {
    private String eventId;
    private String orderId;
    private String orderCode;
    private String userId;
    private BigDecimal totalAmount;
    private String status;
}
