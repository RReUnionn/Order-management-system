package com.vupl.inventoryservice.event.payload;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedPayload {
    private String eventId;
    private String orderId;
    private String userId;
    private List<OrderItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String productId;
        private Integer quantity;
    }
}

