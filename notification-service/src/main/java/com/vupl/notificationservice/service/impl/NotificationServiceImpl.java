package com.vupl.notificationservice.service.impl;

import com.vupl.notificationservice.dto.request.RegisterPushTokenRequest;
import com.vupl.notificationservice.dto.response.*;
import com.vupl.notificationservice.entity.*;
import com.vupl.notificationservice.entity.NotificationTemplate.NotificationChannel;
import com.vupl.notificationservice.event.payload.*;
import com.vupl.notificationservice.exception.AppException;
import com.vupl.notificationservice.repository.*;
import com.vupl.notificationservice.service.NotificationService;
import com.vupl.notificationservice.service.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service @RequiredArgsConstructor @Transactional @Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final PushTokenRepository pushTokenRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final WebSocketPushService webSocketPushService;

    // ── Kafka event handlers ──────────────────────────────────

    @Override
    public void handleOrderEvent(String eventType, OrderEventPayload payload) {
        if (isProcessed(payload.getEventId())) return;

        log.info("Handling {} for orderId={} userId={}", eventType, payload.getOrderId(), payload.getUserId());

        // Render template
        Map<String, String> vars = Map.of(
                "orderCode",    nvl(payload.getOrderCode()),
                "totalAmount",  payload.getTotalAmount() != null ? payload.getTotalAmount().toPlainString() : "",
                "reason",       nvl(payload.getReason())
        );

        templateRepository.findByEventTypeAndChannelAndIsActiveTrue(eventType, NotificationChannel.WEBSOCKET)
                .ifPresent(template -> {
                    String title   = render(template.getTitleTemplate(), vars);
                    String message = render(template.getBodyTemplate(), vars);

                    Notification notification = Notification.builder()
                            .userId(payload.getUserId()).templateId(template.getId())
                            .channel(NotificationChannel.WEBSOCKET)
                            .title(title).message(message)
                            .referenceType("ORDER").referenceId(payload.getOrderId())
                            .sentAt(LocalDateTime.now())
                            .build();

                    Notification saved = notificationRepository.save(notification);

                    // Push qua WebSocket ngay lập tức
                    webSocketPushService.pushToUser(payload.getUserId(),
                            WebSocketMessage.builder()
                                    .notificationId(saved.getId())
                                    .userId(payload.getUserId())
                                    .type(eventType.toUpperCase().replace(".", "_"))
                                    .title(title).message(message)
                                    .referenceType("ORDER").referenceId(payload.getOrderId())
                                    .createdAt(saved.getCreatedAt())
                                    .build());
                });

        markProcessed(payload.getEventId(), eventType);
    }

    @Override
    public void handlePaymentEvent(String eventType, PaymentEventPayload payload) {
        if (isProcessed(payload.getEventId())) return;

        log.info("Handling {} for orderId={} userId={}", eventType, payload.getOrderId(), payload.getUserId());

        Map<String, String> vars = Map.of(
                "orderId",  nvl(payload.getOrderId()),
                "amount",   payload.getAmount() != null ? payload.getAmount().toPlainString() : "",
                "method",   nvl(payload.getMethod()),
                "reason",   nvl(payload.getReason())
        );

        templateRepository.findByEventTypeAndChannelAndIsActiveTrue(eventType, NotificationChannel.WEBSOCKET)
                .ifPresent(template -> {
                    String title   = render(template.getTitleTemplate(), vars);
                    String message = render(template.getBodyTemplate(), vars);

                    Notification notification = Notification.builder()
                            .userId(payload.getUserId()).templateId(template.getId())
                            .channel(NotificationChannel.WEBSOCKET)
                            .title(title).message(message)
                            .referenceType("PAYMENT").referenceId(payload.getPaymentId())
                            .sentAt(LocalDateTime.now())
                            .build();

                    Notification saved = notificationRepository.save(notification);

                    webSocketPushService.pushToUser(payload.getUserId(),
                            WebSocketMessage.builder()
                                    .notificationId(saved.getId())
                                    .userId(payload.getUserId())
                                    .type(eventType.toUpperCase().replace(".", "_"))
                                    .title(title).message(message)
                                    .referenceType("PAYMENT").referenceId(payload.getPaymentId())
                                    .createdAt(saved.getCreatedAt())
                                    .build());
                });

        markProcessed(payload.getEventId(), eventType);
    }

    // ── Query operations ──────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(String userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        long unread = notificationRepository.countByUserIdAndIsReadFalse(userId);
        PageResponse<NotificationResponse> response = PageResponse.from(page.map(NotificationResponse::from));
        response.setUnreadCount(unread);
        return response;
    }

    @Override @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> AppException.notFound("Notification không tồn tại"));
        if (!notification.getUserId().equals(userId))
            throw AppException.forbidden("Không có quyền đánh dấu notification này");
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void markAllAsRead(String userId) {
        int updated = notificationRepository.markAllReadByUserId(userId);
        log.info("Marked {} notifications as read for userId={}", updated, userId);
    }

    // ── Push token ────────────────────────────────────────────

    @Override
    public void registerPushToken(String userId, RegisterPushTokenRequest request) {
        pushTokenRepository.findByDeviceToken(request.getDeviceToken())
                .ifPresentOrElse(token -> {
                    token.setUserId(userId);
                    token.setIsActive(true);
                    pushTokenRepository.save(token);
                }, () -> pushTokenRepository.save(PushToken.builder()
                        .userId(userId).deviceToken(request.getDeviceToken())
                        .platform(request.getPlatform()).build()));
    }

    @Override
    public void deregisterPushToken(String deviceToken) {
        pushTokenRepository.findByDeviceToken(deviceToken).ifPresent(token -> {
            token.setIsActive(false);
            pushTokenRepository.save(token);
        });
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Render template với các biến: {{orderCode}} → giá trị thật
     */
    private String render(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private boolean isProcessed(String eventId) {
        if (eventId == null) return false;
        if (processedEventRepository.existsByEventId(eventId)) {
            log.warn("Event {} already processed, skipping", eventId);
            return true;
        }
        return false;
    }

    private void markProcessed(String eventId, String eventType) {
        if (eventId != null && !processedEventRepository.existsByEventId(eventId))
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId).eventType(eventType).build());
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
