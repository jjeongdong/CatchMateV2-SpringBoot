package com.back.catchmate.infrastructure.chat;

import com.back.catchmate.domain.chat.port.ChatHistoryCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisChatHistoryCacheAdapter implements ChatHistoryCachePort {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void evictLatestPage(Long chatRoomId) {
        String pattern = "chatHistory::" + chatRoomId + "_START_*";
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

        List<String> keysToDelete = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keysToDelete.add(cursor.next());
            }
        }

        if (!keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }
    }
}
