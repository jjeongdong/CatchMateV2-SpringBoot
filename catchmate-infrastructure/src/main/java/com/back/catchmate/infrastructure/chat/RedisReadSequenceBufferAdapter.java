package com.back.catchmate.infrastructure.chat;

import com.back.catchmate.domain.chat.port.ReadSequenceBufferPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisReadSequenceBufferAdapter implements ReadSequenceBufferPort {
    private static final String BUFFER_KEY = "chat:read-sequence:buffer";
    private final StringRedisTemplate redisTemplate;

    @Override
    public void buffer(Long chatRoomId, Long userId, Long sequence) {
        String field = chatRoomId + ":" + userId;
        String current = (String) redisTemplate.opsForHash().get(BUFFER_KEY, field);

        if (current == null || Long.parseLong(current) < sequence) {
            redisTemplate.opsForHash().put(BUFFER_KEY, field, String.valueOf(sequence));
        }
    }

    @Override
    public Map<String, Long> drainAll() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(BUFFER_KEY);

        if (entries.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            result.put((String) entry.getKey(), Long.parseLong((String) entry.getValue()));
        }

        Set<Object> fields = entries.keySet();
        redisTemplate.opsForHash().delete(BUFFER_KEY, fields.toArray());

        return result;
    }
}
