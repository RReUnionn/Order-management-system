package com.vupl.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MockGatewayCallbackRequest {
    @NotBlank
    private String gatewayRef;  // mã giao dịch từ gateway
    @NotBlank
    private String orderId;
    private boolean success = true;       // true = thanh toán OK, false = thất bại
    private String failReason;
}
