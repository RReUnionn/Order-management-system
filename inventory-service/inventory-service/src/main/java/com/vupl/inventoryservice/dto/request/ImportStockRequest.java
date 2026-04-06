package com.vupl.inventoryservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ImportStockRequest {
    @NotBlank
    private String productId;
    @NotBlank
    private String warehouseId;
    @NotNull
    @Min(1)
    private Integer quantity;
    private String note;
}