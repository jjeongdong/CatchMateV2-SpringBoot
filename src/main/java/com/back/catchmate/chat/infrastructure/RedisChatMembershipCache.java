package com.back.catchmate.chat.infrastructure;

import com.back.catchmate.chat.domain.ChatMembershipCache;
import com.back.catchmate.chat.domain.MembershipSnapshot;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisChatMembershipCache implements ChatMembershipCache {
    private static final String KEY_PREFIX = "chat:member:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<MembershipSnapshot> find(Long chatRoomId, Long userId) {
        String value = redisTemplate.opsForValue().get(key(chatRoomId, userId));
        if (value == null) {
            return Optional.empty();
        }
        // 형식: "active|readOnly" 예) "1|0"
        String[] parts = value.split("\\|");
        if (parts.length != 2) {
            return Optional.empty();
        }
        return Optional.of(new MembershipSnapshot("1".equals(parts[0]), "1".equals(parts[1])));
    }

    @Override
    public void put(Long chatRoomId, Long userId, MembershipSnapshot snapshot) {
        String value = (snapshot.active() ? "1" : "0") + "|" + (snapshot.readOnly() ? "1" : "0");
        redisTemplate.opsForValue().set(key(chatRoomId, userId), value, TTL);
    }

    @Override
    public void evict(Long chatRoomId, Long userId) {
        redisTemplate.delete(key(chatRoomId, userId));
    }

    private String key(Long chatRoomId, Long userId) {
        return KEY_PREFIX + chatRoomId + ":" + userId;
    }
}
