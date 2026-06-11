package com.back.catchmate.infrastructure.redis;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.application.notification.event.NotificationEvent;
import com.back.catchmate.application.notification.port.NotificationDispatchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher implements NotificationDispatchPort {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic chatTopic;
    private final ChannelTopic notificationTopic;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishChat(ChatMessageEvent event) {
        try {
            redisTemplate.convertAndSend(chatTopic.getTopic(), event);
        } catch (Exception e) {
            log.error("Redis Pub/Sub 장애: 채팅 메시지 전송 실패", e);
        }
    }

    @Override
    public void dispatch(Long userId, Map<String, String> payload) {
        try {
            redisTemplate.convertAndSend(notificationTopic.getTopic(), NotificationEvent.of(userId, payload));
        } catch (Exception e) {
            log.error("Redis Pub/Sub 장애: 알림 전송 실패. userId: {}", userId, e);
        }
    }
}
