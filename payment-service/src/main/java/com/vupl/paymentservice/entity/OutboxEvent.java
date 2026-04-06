package com.vupl.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;
    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    @Column(nullable = false, columnDefinition = "JSON")
    private String payload;
    @Column(nullable = false)
    @Builder.Default
    private Boolean published = false;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
