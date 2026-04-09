package com.vupl.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "push_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushToken {
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    @Column(name = "device_token", nullable = false, unique = true, length = 300)
    private String deviceToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Platform platform;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Platform {ANDROID, IOS, WEB}
}
