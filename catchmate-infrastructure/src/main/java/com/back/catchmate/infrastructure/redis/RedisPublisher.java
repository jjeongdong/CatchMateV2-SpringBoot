package com.back.catchmate.infrastructure.redis;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.application.chat.event.ChatReadEvent;
import com.back.catchmate.application.chat.port.MessagePublisher;
import com.back.catchmate.application.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher implements MessagePublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic chatTopic;
    private final ChannelTopic notificationTopic;
    private final ChannelTopic readTopic;

    @Override
    public void publishChat(ChatMessageEvent event) {
        redisTemplate.convertAndSend(chatTopic.getTopic(), event);
    }

    @Override
    public void publishNotification(NotificationEvent event) {
        try {
            redisTemplate.convertAndSend(notificationTopic.getTopic(), event);
        } catch (Exception e) {
            log.error("Redis Pub/Sub 장애: 알림 전송 실패", e);
        }
    }

    @Override
    public void publishRead(ChatReadEvent event) {
        try {
            redisTemplate.convertAndSend(readTopic.getTopic(), event);
        } catch (Exception e) {
            log.warn("Redis Pub/Sub 장애: 읽음 처리 전송 실패", e);
        }
    }
}
