package com.back.catchmate.user.service;

import com.back.catchmate.user.infra.RedisUserOnlineStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Redis 전용 서비스라 DB 를 건드리지 않는다.
 * WebSocket connect/disconnect 와 알림 발송 경로에서 매우 자주 호출되므로
 * @Transactional 을 붙이지 않는다 (붙이면 호출마다 DB 커넥션을 점유한다).
 */
@Service
@RequiredArgsConstructor
public class UserOnlineStatusService {
    private final RedisUserOnlineStatus redisUserOnlineStatus;

    public void setUserOnline(Long userId) {
        redisUserOnlineStatus.setUserOnline(userId);
    }

    public void setUserOffline(Long userId) {
        redisUserOnlineStatus.setUserOffline(userId);
    }

    public void setUserFocusRoom(Long userId, Long roomId) {
        redisUserOnlineStatus.setUserFocusRoom(userId, roomId);
    }

    public void removeUserFocusRoom(Long userId) {
        redisUserOnlineStatus.removeUserFocusRoom(userId);
    }

    public boolean isUserOnline(Long userId) {
        return redisUserOnlineStatus.isUserOnline(userId);
    }

    public Long getUserFocusRoom(Long userId) {
        return redisUserOnlineStatus.getUserFocusRoom(userId);
    }

    public Map<Long, Long> getUserFocusRooms(List<Long> userIds) {
        return redisUserOnlineStatus.getUserFocusRooms(userIds);
    }
}
