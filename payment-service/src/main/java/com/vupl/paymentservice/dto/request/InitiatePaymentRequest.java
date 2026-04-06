package com.vupl.paymentservice.dto.request;

import com.vupl.paymentservice.entity.Payment.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitiatePaymentRequest {
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod method;
    private String returnUrl; // URL redirect sau khi thanh toán (cho VNPay, Momo)
}
