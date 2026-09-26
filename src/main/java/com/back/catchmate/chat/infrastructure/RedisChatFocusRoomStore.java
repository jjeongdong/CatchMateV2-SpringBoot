package com.back.catchmate.chat.infrastructure;

import com.back.catchmate.chat.domain.ChatFocusRoomStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis 장애가 채팅·알림 전체로 번지지 않도록 예외를 잡고 안전한 기본값을 돌려준다.
 * 키는 TTL 없이 세팅되고 WebSocket 연결 끊김·구독 취소에서 지워진다 (비정상 종료는 STOMP 하트비트가 정리한다).
 * 키 이름은 user BC 에 있던 시절과 같아 배포 중에도 값이 이어진다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisChatFocusRoomStore implements ChatFocusRoomStore {
    private static final String FOCUS_KEY_PREFIX = "user:focus:";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void focus(Long userId, Long chatRoomId) {
        try {
            redisTemplate.opsForValue().set(FOCUS_KEY_PREFIX + userId, chatRoomId.toString());
        } catch (Exception e) {
            // 던지지 않아야 웹소켓 구독이 끊기지 않는다.
            log.error("Redis 장애: 사용자 {} 포커스 방({}) 설정 실패 - {}", userId, chatRoomId, e.getMessage());
        }
    }

    @Override
    public void unfocus(Long userId) {
        try {
            redisTemplate.delete(FOCUS_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.error("Redis 장애: 사용자 {} 포커스 방 제거 실패 - {}", userId, e.getMessage());
        }
    }

    @Override
    public Optional<Long> find(Long userId) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(FOCUS_KEY_PREFIX + userId))
                    .map(Long::parseLong);
        } catch (Exception e) {
            // 포커스 중인 방이 없다고 간주해 일반 알림 발송 경로를 타게 한다.
            log.error("Redis 장애: 사용자 {} 포커스 방 조회 실패. 빈 값 반환 - {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 수신자별 GET 대신 MGET 한 번으로 묶는다.
     * MGET 응답은 요청한 키 순서대로 오고 없는 키 자리에는 null 이 들어오므로, 인덱스로 userIds 와 짝을 맞춘다.
     */
    @Override
    public Map<Long, Long> findAll(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        try {
            List<String> keys =
                    userIds.stream().map(id -> FOCUS_KEY_PREFIX + id).toList();
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                return Map.of();
            }
            Map<Long, Long> roomIdByUserId = new HashMap<>();
            for (int i = 0; i < userIds.size(); i++) {
                String value = values.get(i);
                if (value != null) {
                    roomIdByUserId.put(userIds.get(i), Long.parseLong(value));
                }
            }
            return roomIdByUserId;
        } catch (Exception e) {
            // 단건 조회와 같은 방향: 아무도 포커스 중이 아니라고 간주해 알림이 계속 나가게 한다.
            log.error("Redis 장애: 사용자 {}명 포커스 방 일괄 조회 실패. 빈 Map 반환 - {}", userIds.size(), e.getMessage());
            return Map.of();
        }
    }
}
