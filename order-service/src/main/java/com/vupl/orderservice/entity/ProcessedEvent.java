package com.vupl.orderservice.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="processed_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessedEvent {
    @Id @Column(name="event_id", length=36) private String eventId;
    @Column(name="event_type", nullable=false, length=100) private String eventType;
    @Column(name="processed_at", nullable=false) private LocalDateTime processedAt;
    @PrePersist protected void onCreate() { processedAt=LocalDateTime.now(); }
}
