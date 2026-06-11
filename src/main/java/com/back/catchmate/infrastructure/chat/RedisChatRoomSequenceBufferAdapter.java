package com.back.catchmate.infrastructure.chat;

import com.back.catchmate.domain.chat.port.ChatRoomSequenceBufferPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatRoomSequenceBufferAdapter implements ChatRoomSequenceBufferPort {
    private static final String BUFFER_KEY = "chat:room-sequence:buffer";
    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> BUFFER_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('HGET', KEYS[1], ARGV[1]) " +
                    "if current == false or tonumber(current) < tonumber(ARGV[2]) then " +
                    "  redis.call('HSET', KEYS[1], ARGV[1], ARGV[2]) " +
                    "  return 1 " +
                    "end " +
                    "return 0",
            Long.class
    );

    private static final DefaultRedisScript<List> DRAIN_SCRIPT = new DefaultRedisScript<>(
            "local entries = redis.call('HGETALL', KEYS[1]) " +
                    "if #entries > 0 then " +
                    "  redis.call('DEL', KEYS[1]) " +
                    "end " +
                    "return entries",
            List.class
    );

    @Override
    public void buffer(Long chatRoomId, Long sequence) {
        redisTemplate.execute(BUFFER_SCRIPT, Collections.singletonList(BUFFER_KEY),
                String.valueOf(chatRoomId), String.valueOf(sequence));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, Long> drainAll() {
        List<String> entries = redisTemplate.execute(DRAIN_SCRIPT, Collections.singletonList(BUFFER_KEY));

        if (entries == null || entries.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> result = new HashMap<>();
        for (int i = 0; i < entries.size(); i += 2) {
            Long chatRoomId = Long.parseLong(entries.get(i));
            Long sequence = Long.parseLong(entries.get(i + 1));
            result.put(chatRoomId, sequence);
        }

        return result;
    }
}
