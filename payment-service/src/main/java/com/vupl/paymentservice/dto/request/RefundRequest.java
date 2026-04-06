package com.vupl.paymentservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
    @NotNull
    @DecimalMin("1000")
    private BigDecimal amount;
    @NotBlank
    private String reason;
}
