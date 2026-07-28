package com.back.catchmate.user.adapter.out.external;

import com.back.catchmate.user.application.port.out.external.UserOnlineStatusPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis 기반 사용자 온라인/오프라인 상태 추적 서비스
 * 고가용성(HA) 확보: Redis 장애 발생 시 시스템 전체로 장애가 전파되지 않도록 예외를 잡고 안전한 기본값을 반환합니다.
 * 온라인 키는 TTL 없이 세팅되며, WebSocket Disconnect 이벤트에서만 제거됩니다.
 * 비정상 종료로 Disconnect 이벤트가 누락되어 stale 상태가 남아있을 위험이 있으나
 * 일반적인 흐름에서는 Spring 의 SessionDisconnectEvent 가 발생합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUserOnlineStatus implements UserOnlineStatusPort {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String ONLINE_USER_KEY_PREFIX = "user:online:";
    private static final String USER_ROOM_FOCUS_KEY_PREFIX = "user:focus:";

    @Override
    public void setUserOnline(Long userId) {
        try {
            String key = ONLINE_USER_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(key, "true");
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
            redisTemplate.opsForValue().set(key, roomId.toString());
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

    /**
     * 수신자별로 GET 을 반복하는 대신 MGET 한 번으로 묶는다.
     * MGET 응답은 요청한 키 순서대로 오고 없는 키 자리에는 null 이 들어오므로, 인덱스로 userIds 와 짝을 맞춘다.
     */
    @Override
    public Map<Long, Long> getUserFocusRooms(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();

        try {
            List<String> keys = userIds.stream().map(id -> USER_ROOM_FOCUS_KEY_PREFIX + id).toList();
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) return Map.of();

            Map<Long, Long> focusRooms = new HashMap<>();
            for (int i = 0; i < userIds.size(); i++) {
                String val = values.get(i);
                if (val != null) focusRooms.put(userIds.get(i), Long.parseLong(val));
            }
            return focusRooms;
        } catch (Exception e) {
            log.error("Redis 장애: 사용자 {}명 포커스 방 일괄 조회 실패. 기본값(빈 Map) 반환 - {}", userIds.size(), e.getMessage());
            // 단건 조회와 같은 방향: 아무도 포커스 중이 아니라고 간주하여 알림이 계속 나가게 함
            return Map.of();
        }
    }
}
