package com.back.catchmate.chat.infrastructure;

import com.back.catchmate.chat.domain.ChatRoomSequenceBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisChatRoomSequenceBuffer implements ChatRoomSequenceBuffer {
    private static final String BUFFER_KEY = "chat:room-sequence:buffer";

    // 더 큰 값일 때만 덮어쓴다 (여러 서버가 순서 없이 buffer 해도 역전되지 않게).
    private static final DefaultRedisScript<Long> BUFFER_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('HGET', KEYS[1], ARGV[1]) "
                    + "if current == false or tonumber(current) < tonumber(ARGV[2]) then "
                    + "  redis.call('HSET', KEYS[1], ARGV[1], ARGV[2]) "
                    + "  return 1 "
                    + "end "
                    + "return 0",
            Long.class);

    // 읽기와 삭제를 한 번에 해 드레인 사이에 들어온 값을 잃지 않는다.
    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> DRAIN_SCRIPT = new DefaultRedisScript<>(
            "local entries = redis.call('HGETALL', KEYS[1]) " + "if #entries > 0 then "
                    + "  redis.call('DEL', KEYS[1]) "
                    + "end "
                    + "return entries",
            List.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void buffer(Long chatRoomId, Long sequence) {
        redisTemplate.execute(
                BUFFER_SCRIPT,
                Collections.singletonList(BUFFER_KEY),
                String.valueOf(chatRoomId),
                String.valueOf(sequence));
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
            result.put(Long.parseLong(entries.get(i)), Long.parseLong(entries.get(i + 1)));
        }
        return result;
    }
}
