package com.vupl.paymentservice.dto.response;

import com.vupl.paymentservice.entity.PaymentEvent;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentEventResponse {
    private Long id;
    private String eventType;
    private String gatewayCode;
    private String gatewayMessage;
    private LocalDateTime createdAt;

    public static PaymentEventResponse from(PaymentEvent e) {
        return PaymentEventResponse.builder()
                .id(e.getId()).eventType(e.getEventType())
                .gatewayCode(e.getGatewayCode()).gatewayMessage(e.getGatewayMessage())
                .createdAt(e.getCreatedAt()).build();
    }
}
