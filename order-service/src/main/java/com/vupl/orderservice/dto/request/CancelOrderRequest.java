package com.vupl.orderservice.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class CancelOrderRequest {
    @NotBlank(message = "Lý do hủy không được để trống")
    private String reason;
}
