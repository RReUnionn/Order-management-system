package com.vupl.notificationservice.service;

import com.vupl.notificationservice.event.payload.WebSocketMessage;

public interface WebSocketPushService {
    void pushToUser(String userId, WebSocketMessage message);

    void pushToAll(WebSocketMessage message);
}
