package com.back.catchmate.chat.infrastructure;

import com.back.catchmate.chat.domain.ChatSequenceStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisChatSequenceStore implements ChatSequenceStore {
    private static final String SEQ_KEY_PREFIX = "chat:room:";
    private static final String SEQ_KEY_SUFFIX = ":seq";

    private final StringRedisTemplate redisTemplate;

    @Override
    public Long next(Long chatRoomId) {
        return redisTemplate.opsForValue().increment(SEQ_KEY_PREFIX + chatRoomId + SEQ_KEY_SUFFIX);
    }

    @Override
    public Long current(Long chatRoomId) {
        String value = redisTemplate.opsForValue().get(SEQ_KEY_PREFIX + chatRoomId + SEQ_KEY_SUFFIX);
        return value != null ? Long.parseLong(value) : 0L;
    }
}
