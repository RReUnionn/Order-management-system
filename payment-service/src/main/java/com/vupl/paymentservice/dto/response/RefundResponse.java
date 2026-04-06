package com.vupl.paymentservice.dto.response;

import com.vupl.paymentservice.entity.Refund;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RefundResponse {
    private String id;
    private String paymentId;
    private BigDecimal amount;
    private String reason;
    private String status;
    private String gatewayRef;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;

    public static RefundResponse from(Refund r) {
        return RefundResponse.builder()
                .id(r.getId()).paymentId(r.getPaymentId()).amount(r.getAmount())
                .reason(r.getReason()).status(r.getStatus().name())
                .gatewayRef(r.getGatewayRef()).processedAt(r.getProcessedAt())
                .createdAt(r.getCreatedAt()).build();
    }
}
