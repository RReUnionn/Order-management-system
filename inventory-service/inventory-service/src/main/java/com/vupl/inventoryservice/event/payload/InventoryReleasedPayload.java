package com.vupl.inventoryservice.event.payload;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReleasedPayload {
    private String eventId;
    private String orderId;
}
