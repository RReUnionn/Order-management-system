package com.vupl.notificationservice.config;

import com.vupl.notificationservice.event.RedisNotificationSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.*;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration @RequiredArgsConstructor
public class RedisConfig {

    private final RedisNotificationSubscriber subscriber;

    @Value("${websocket.redis-channel:notification:broadcast}")
    private String redisChannel;

    @Bean
    public MessageListenerAdapter messageListenerAdapter() {
        return new MessageListenerAdapter(subscriber);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // Subscribe vào channel để nhận broadcast từ các instance khác
        container.addMessageListener(messageListenerAdapter(),
                new PatternTopic(redisChannel));
        return container;
    }
}
