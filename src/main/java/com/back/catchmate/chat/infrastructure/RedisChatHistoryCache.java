package com.back.catchmate.chat.infrastructure;

import com.back.catchmate.chat.domain.ChatHistoryCache;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisChatHistoryCache implements ChatHistoryCache {
    private final RedisTemplate<String, Object> redisTemplate;

    // 키 형식은 ChatHistoryReader 의 @Cacheable 키와 짝이다: chatHistory::{roomId}_START_{limit}
    @Override
    public void evictLatestPage(Long chatRoomId) {
        String pattern = "chatHistory::" + chatRoomId + "_START_*";
        ScanOptions options =
                ScanOptions.scanOptions().match(pattern).count(100).build();
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
