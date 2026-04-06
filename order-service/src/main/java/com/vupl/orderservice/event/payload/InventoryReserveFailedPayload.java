package com.vupl.orderservice.event.payload;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReserveFailedPayload {
    private String eventId;
    private String orderId;
    private String reason;
    private String productId;
}
