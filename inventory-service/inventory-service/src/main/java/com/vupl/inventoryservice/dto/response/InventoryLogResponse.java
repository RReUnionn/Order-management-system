package com.vupl.inventoryservice.dto.response;

import com.vupl.inventoryservice.entity.InventoryLog;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryLogResponse {
    private Long id;
    private String productId;
    private Integer delta;
    private Integer qtyAfter;
    private String reason;
    private String referenceId;
    private String note;
    private LocalDateTime createdAt;

    public static InventoryLogResponse from(InventoryLog log) {
        return InventoryLogResponse.builder()
                .id(log.getId()).productId(log.getProductId()).delta(log.getDelta())
                .qtyAfter(log.getQtyAfter()).reason(log.getReason().name())
                .referenceId(log.getReferenceId()).note(log.getNote()).createdAt(log.getCreatedAt()).build();
    }
}
