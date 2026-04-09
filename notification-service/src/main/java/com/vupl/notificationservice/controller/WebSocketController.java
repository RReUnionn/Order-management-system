package com.vupl.notificationservice.controller;

import com.vupl.notificationservice.event.payload.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    /**
     * Client subscribe vào /user/queue/notifications để nhận notifications
     * Khi subscribe lần đầu, gửi lại welcome message để xác nhận kết nối
     */
    @SubscribeMapping("/queue/notifications")
    public WebSocketMessage onSubscribe(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Client subscribed to notifications, sessionId={}", sessionId);
        return WebSocketMessage.builder()
                .type("CONNECTED")
                .title("Kết nối thành công")
                .message("Bạn sẽ nhận thông báo realtime từ hệ thống")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Client ping để giữ kết nối
     * Client gửi tới: /app/ping
     * Server trả về: /user/queue/pong
     */
    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    public Map<String, Object> ping() {
        return Map.of("type", "PONG", "timestamp", LocalDateTime.now().toString());
    }
}
