package com.vupl.notificationservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vupl.notificationservice.event.payload.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisNotificationSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Nhận message từ Redis pub/sub và forward tới WebSocket client
     * Đây là cơ chế để nhiều instance của Notification Service cùng push được
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            WebSocketMessage wsMessage = objectMapper.readValue(
                    message.getBody(), WebSocketMessage.class);

            if (wsMessage.getUserId() != null) {
                // Push tới user cụ thể
                messagingTemplate.convertAndSendToUser(
                        wsMessage.getUserId(), "/queue/notifications", wsMessage);
            } else {
                // Broadcast tới tất cả
                messagingTemplate.convertAndSend("/topic/broadcast", wsMessage);
            }
        } catch (Exception e) {
            log.error("Error processing Redis notification message: {}", e.getMessage());
        }
    }
}
