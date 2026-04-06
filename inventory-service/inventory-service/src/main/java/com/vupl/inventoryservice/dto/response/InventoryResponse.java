package com.vupl.inventoryservice.dto.response;
import com.vupl.inventoryservice.entity.Inventory;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder
public class InventoryResponse {
    private String id;
    private String productId;
    private String warehouseId;
    private String warehouseName;
    private Integer quantity;
    private Integer reservedQty;
    private Integer availableQty;
    private Integer reorderPoint;
    private LocalDateTime updatedAt;
    public static InventoryResponse from(Inventory inv) {
        return InventoryResponse.builder()
                .id(inv.getId()).productId(inv.getProductId())
                .warehouseId(inv.getWarehouse().getId()).warehouseName(inv.getWarehouse().getName())
                .quantity(inv.getQuantity()).reservedQty(inv.getReservedQty())
                .availableQty(inv.getAvailableQty()).reorderPoint(inv.getReorderPoint())
                .updatedAt(inv.getUpdatedAt()).build();
    }
}
