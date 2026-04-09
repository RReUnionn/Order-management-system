package com.vupl.notificationservice.service;

import com.vupl.notificationservice.dto.request.RegisterPushTokenRequest;
import com.vupl.notificationservice.dto.response.*;
import com.vupl.notificationservice.event.payload.*;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    // Kafka consumers
    void handleOrderEvent(String eventType, OrderEventPayload payload);

    void handlePaymentEvent(String eventType, PaymentEventPayload payload);

    // Customer APIs
    PageResponse<NotificationResponse> getMyNotifications(String userId, Pageable pageable);

    long getUnreadCount(String userId);

    void markAsRead(String notificationId, String userId);

    void markAllAsRead(String userId);

    // Push token
    void registerPushToken(String userId, RegisterPushTokenRequest request);

    void deregisterPushToken(String deviceToken);
}
