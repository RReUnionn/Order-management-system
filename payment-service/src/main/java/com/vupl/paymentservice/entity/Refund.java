package com.vupl.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(name = "payment_id", nullable = false, length = 36)
    private String paymentId;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 300)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;

    @Column(name = "gateway_ref", length = 100)
    private String gatewayRef;
    @Column(name = "requested_by", length = 36)
    private String requestedBy;
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum RefundStatus {PENDING, PROCESSING, COMPLETED, FAILED}
}
