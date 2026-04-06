package com.vupl.paymentservice.dto.response;

import com.vupl.paymentservice.entity.Payment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private String id;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String method;
    private String status;
    private String gatewayRef;
    private String gatewayUrl;
    private LocalDateTime paidAt;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;

    public static PaymentResponse from(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId()).orderId(p.getOrderId()).userId(p.getUserId())
                .amount(p.getAmount()).currency(p.getCurrency())
                .method(p.getMethod().name()).status(p.getStatus().name())
                .gatewayRef(p.getGatewayRef()).gatewayUrl(p.getGatewayUrl())
                .paidAt(p.getPaidAt()).expiredAt(p.getExpiredAt())
                .createdAt(p.getCreatedAt()).build();
    }
}
