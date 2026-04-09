package com.vupl.notificationservice.repository;

import com.vupl.notificationservice.entity.NotificationTemplate;
import com.vupl.notificationservice.entity.NotificationTemplate.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {
    Optional<NotificationTemplate> findByEventTypeAndChannelAndIsActiveTrue(String eventType, NotificationChannel channel);
}
