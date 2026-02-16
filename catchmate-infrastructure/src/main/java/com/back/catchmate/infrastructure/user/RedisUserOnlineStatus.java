package com.back.catchmate.infrastructure.user;

import com.back.catchmate.domain.user.port.UserOnlineStatusPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 기반 사용자 온라인/오프라인 상태 추적 서비스
 * WebSocket 연결 시 온라인으로 설정, 연결 해제 시 오프라인으로 설정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisUserOnlineStatus implements UserOnlineStatusPort {
    private final RedisTemplate<String, String> redisTemplate;

    private static final String ONLINE_USER_KEY_PREFIX = "user:online:";
    private static final String USER_ROOM_FOCUS_KEY_PREFIX = "user:focus:";
    private static final Duration ONLINE_EXPIRE_TIME = Duration.ofMinutes(5);

    @Override
    public void setUserOnline(Long userId) {
        String key = ONLINE_USER_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, "true", ONLINE_EXPIRE_TIME);
        log.debug("User {} is now ONLINE", userId);
    }

    @Override
    public void setUserOffline(Long userId) {
        String key = ONLINE_USER_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("User {} is now OFFLINE", userId);
    }

    @Override
    public boolean isUserOnline(Long userId) {
        String key = ONLINE_USER_KEY_PREFIX + userId;
        return redisTemplate.hasKey(key);
    }

    @Override
    public void setUserFocusRoom(Long userId, Long roomId) {
        String key = USER_ROOM_FOCUS_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, roomId.toString(), ONLINE_EXPIRE_TIME);
    }

    @Override
    public void removeUserFocusRoom(Long userId) {
        redisTemplate.delete(USER_ROOM_FOCUS_KEY_PREFIX + userId);
    }

    @Override
    public Long getUserFocusRoom(Long userId) {
        String val = redisTemplate.opsForValue().get(USER_ROOM_FOCUS_KEY_PREFIX + userId);
        return val != null ? Long.parseLong(val) : null;
    }
}
