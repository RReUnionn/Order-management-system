package com.vupl.notificationservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vupl.notificationservice.event.payload.WebSocketMessage;
import com.vupl.notificationservice.service.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor @Slf4j
public class WebSocketPushServiceImpl implements WebSocketPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${websocket.redis-channel:notification:broadcast}")
    private String redisChannel;

    /**
     * Push thẳng tới user qua WebSocket (STOMP destination: /user/{userId}/queue/notifications)
     * Đồng thời publish lên Redis pub/sub để các instance khác cũng push được
     */
    @Override
    public void pushToUser(String userId, WebSocketMessage message) {
        // 1. Push trực tiếp qua WebSocket (nếu user đang kết nối vào instance này)
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", message);
        log.info("Pushed WebSocket notification to userId={} type={}", userId, message.getType());

        // 2. Publish lên Redis để các instance khác forward tới user
        publishToRedis(message);
    }

    @Override
    public void pushToAll(WebSocketMessage message) {
        messagingTemplate.convertAndSend("/topic/broadcast", message);
        publishToRedis(message);
    }

    private void publishToRedis(WebSocketMessage message) {
        try {
            redisTemplate.convertAndSend(redisChannel, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.error("Failed to publish to Redis: {}", e.getMessage());
        }
    }
}
