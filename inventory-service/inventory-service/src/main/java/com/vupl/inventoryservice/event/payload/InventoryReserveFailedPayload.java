package com.vupl.inventoryservice.event.payload;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReserveFailedPayload {
    private String eventId;
    private String orderId;
    private String reason;
    private String productId;
}
