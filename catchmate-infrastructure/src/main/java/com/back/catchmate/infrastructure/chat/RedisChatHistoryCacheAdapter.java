package com.back.catchmate.infrastructure.chat;

import com.back.catchmate.domain.chat.port.ChatHistoryCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisChatHistoryCacheAdapter implements ChatHistoryCachePort {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void evictLatestPage(Long chatRoomId) {
        Set<String> keys = redisTemplate.keys("chatHistory::" + chatRoomId + "_START_*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
