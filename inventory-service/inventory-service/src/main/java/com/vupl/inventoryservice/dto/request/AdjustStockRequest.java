package com.vupl.inventoryservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdjustStockRequest {
    @NotNull
    private Integer delta;
    @NotBlank
    private String reason;
    private String note;
}
