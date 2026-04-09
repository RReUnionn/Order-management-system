package com.vupl.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    @Column(name = "template_id", length = 36)
    private String templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private NotificationTemplate.NotificationChannel channel;

    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "reference_type", length = 50)
    private String referenceType;
    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;
    @Column(name = "read_at")
    private LocalDateTime readAt;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
