package com.back.catchmate.infrastructure.chat;

import com.back.catchmate.domain.chat.port.ReadSequenceBufferPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Race-prone V3 implementation: GET → compare → PUT and HGETALL → DEL split into
// non-atomic Java steps. Kept for portfolio measurement of the race condition that
// motivated the move to the Lua-script adapter (V4).
@Slf4j
@Component("javaReadSequenceBufferAdapter")
@RequiredArgsConstructor
public class JavaReadSequenceBufferAdapter implements ReadSequenceBufferPort {
    private static final String BUFFER_KEY = "chat:read-sequence:buffer:v3";
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
    public Map<Long, Long> getBufferedSequences(List<Long> chatRoomIds, Long userId) {
        if (chatRoomIds == null || chatRoomIds.isEmpty()) {
            return Map.of();
        }

        List<Object> fields = chatRoomIds.stream()
                .map(chatRoomId -> chatRoomId + ":" + userId)
                .map(field -> (Object) field)
                .toList();
        List<Object> values = redisTemplate.opsForHash().multiGet(BUFFER_KEY, fields);

        if (values == null || values.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> result = new HashMap<>();
        for (int i = 0; i < chatRoomIds.size() && i < values.size(); i++) {
            Object value = values.get(i);
            if (value != null) {
                try {
                    result.put(chatRoomIds.get(i), Long.parseLong(value.toString()));
                } catch (NumberFormatException e) {
                    log.warn("읽음 시퀀스 버퍼 값 파싱 실패 (roomId: {}, value: {})", chatRoomIds.get(i), value);
                }
            }
        }
        return result;
    }

    @Override
    public int size() {
        Long n = redisTemplate.opsForHash().size(BUFFER_KEY);
        return n == null ? 0 : n.intValue();
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
