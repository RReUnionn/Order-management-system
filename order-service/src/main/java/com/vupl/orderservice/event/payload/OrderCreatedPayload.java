package com.vupl.orderservice.event.payload;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class OrderCreatedPayload {
    private String eventId;
    private String orderId;
    private String orderCode;
    private String userId;
    private BigDecimal totalAmount;
    private List<OrderItemPayload> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemPayload {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
