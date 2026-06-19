package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.notification.application.event.NotificationEvent;
import com.back.catchmate.notification.application.port.out.external.NotificationDispatchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisNotificationPublisher implements NotificationDispatchPort {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic notificationTopic;

    @Override
    public void dispatch(Long userId, Map<String, String> payload) {
        try {
            redisTemplate.convertAndSend(notificationTopic.getTopic(), NotificationEvent.of(userId, payload));
        } catch (Exception e) {
            log.error("Redis Pub/Sub 장애: 알림 전송 실패. userId: {}", userId, e);
        }
    }
}
