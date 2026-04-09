package com.vupl.notificationservice.dto.response;

import com.vupl.notificationservice.entity.Notification;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String title;
    private String message;
    private String channel;
    private String referenceType;
    private String referenceId;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).title(n.getTitle()).message(n.getMessage())
                .channel(n.getChannel().name()).referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId()).isRead(n.getIsRead())
                .readAt(n.getReadAt()).createdAt(n.getCreatedAt()).build();
    }
}
