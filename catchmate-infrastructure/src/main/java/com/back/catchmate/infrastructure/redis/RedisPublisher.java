package com.back.catchmate.infrastructure.redis;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.application.chat.port.MessagePublisherPort;
import com.back.catchmate.application.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher implements MessagePublisherPort {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic chatTopic;
    private final ChannelTopic notificationTopic;

    @Override
    public void publishChat(ChatMessageEvent event) {
        try {
            redisTemplate.convertAndSend(chatTopic.getTopic(), event);
        } catch (Exception e) {
            log.error("Redis Pub/Sub 장애: 채팅 메시지 전송 실패", e);
        }
    }

    @Override
    public void publishNotification(NotificationEvent event) {
        try {
            redisTemplate.convertAndSend(notificationTopic.getTopic(), event);
        } catch (Exception e) {
            log.error("Redis Pub/Sub 장애: 알림 전송 실패", e);
        }
    }
}
