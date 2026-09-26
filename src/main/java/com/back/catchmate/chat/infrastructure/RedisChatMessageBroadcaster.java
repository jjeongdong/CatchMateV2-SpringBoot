package com.back.catchmate.chat.infrastructure;

import com.back.catchmate.chat.domain.ChatMessageBroadcaster;
import com.back.catchmate.chat.domain.event.ChatMessageBroadcastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMessageBroadcaster implements ChatMessageBroadcaster {
    private final RedisTemplate<String, ChatMessageBroadcastEvent> chatPubSubRedisTemplate;
    private final ChannelTopic chatTopic;

    // 커밋 후라 메시지는 이미 확정이다. 방송 실패는 로그만 남긴다 (재연결 시 sync API 로 복구된다).
    @Override
    public void broadcast(ChatMessageBroadcastEvent event) {
        try {
            chatPubSubRedisTemplate.convertAndSend(chatTopic.getTopic(), event);
        } catch (Exception e) {
            log.error("Redis Pub/Sub 장애: 채팅 메시지 전송 실패", e);
        }
    }
}
