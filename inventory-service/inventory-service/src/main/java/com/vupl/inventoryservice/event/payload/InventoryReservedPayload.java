package com.vupl.inventoryservice.event.payload;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedPayload {
    private String eventId;
    private String orderId;
    private boolean success;
    private List<ReservedItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservedItem {
        private String productId;
        private Integer quantity;
    }
}
