package com.back.catchmate.infrastructure.user;

import com.back.catchmate.domain.user.port.UserOnlineStatusPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 기반 사용자 온라인/오프라인 상태 추적 서비스
 * 고가용성(HA) 확보: Redis 장애 발생 시 시스템 전체로 장애가 전파되지 않도록 예외를 잡고 안전한 기본값을 반환합니다.
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
        try {
            String key = ONLINE_USER_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(key, "true", ONLINE_EXPIRE_TIME);
            log.debug("User {} is now ONLINE", userId);
        } catch (Exception e) {
            log.error("Redis 장애: 사용자 {} 온라인 상태 설정 실패 - {}", userId, e.getMessage());
            // 에러를 던지지 않고 무시하여 웹소켓 연결이 끊어지는 것을 방지
        }
    }

    @Override
    public void setUserOffline(Long userId) {
        try {
            String key = ONLINE_USER_KEY_PREFIX + userId;
            redisTemplate.delete(key);
            log.debug("User {} is now OFFLINE", userId);
        } catch (Exception e) {
            log.error("Redis 장애: 사용자 {} 오프라인 상태 설정 실패 - {}", userId, e.getMessage());
        }
    }

    @Override
    public boolean isUserOnline(Long userId) {
        try {
            String key = ONLINE_USER_KEY_PREFIX + userId;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis 장애: 사용자 {} 온라인 상태 조회 실패. 기본값(false) 반환 - {}", userId, e.getMessage());
            // 중요: 장애 시 오프라인(false)으로 간주하여, 알림 서비스가 FCM 푸시 알림을 정상적으로 발송하도록 유도
            return false;
        }
    }

    @Override
    public void setUserFocusRoom(Long userId, Long roomId) {
        try {
            String key = USER_ROOM_FOCUS_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(key, roomId.toString(), ONLINE_EXPIRE_TIME);
        } catch (Exception e) {
            log.error("Redis 장애: 사용자 {} 포커스 방({}) 설정 실패 - {}", userId, roomId, e.getMessage());
        }
    }

    @Override
    public void removeUserFocusRoom(Long userId) {
        try {
            redisTemplate.delete(USER_ROOM_FOCUS_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.error("Redis 장애: 사용자 {} 포커스 방 제거 실패 - {}", userId, e.getMessage());
        }
    }

    @Override
    public Long getUserFocusRoom(Long userId) {
        try {
            String val = redisTemplate.opsForValue().get(USER_ROOM_FOCUS_KEY_PREFIX + userId);
            return val != null ? Long.parseLong(val) : null;
        } catch (Exception e) {
            log.error("Redis 장애: 사용자 {} 포커스 방 조회 실패. 기본값(null) 반환 - {}", userId, e.getMessage());
            // 포커스 중인 방이 없다고 간주하여 일반적인 알림 발송 로직을 타게 함
            return null;
        }
    }
}
