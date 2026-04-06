package com.vupl.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, length = 36)
    private String paymentId;
    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;
    @Column(name = "gateway_code", length = 20)
    private String gatewayCode;
    @Column(name = "gateway_message", length = 200)
    private String gatewayMessage;
    @Column(columnDefinition = "JSON")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
