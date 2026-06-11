package com.back.catchmate.global.redis;

import com.back.catchmate.chat.application.event.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageRedisPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic chatTopic;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishChat(ChatMessageEvent event) {
        try {
            redisTemplate.convertAndSend(chatTopic.getTopic(), event);
        } catch (Exception e) {
            log.error("Redis Pub/Sub 장애: 채팅 메시지 전송 실패", e);
        }
    }
}
