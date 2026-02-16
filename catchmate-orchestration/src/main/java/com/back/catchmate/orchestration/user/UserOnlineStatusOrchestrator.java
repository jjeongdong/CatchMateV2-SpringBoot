package com.back.catchmate.orchestration.user;

import com.back.catchmate.domain.user.port.UserOnlineStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 사용자 온라인 상태 관리 Orchestrator (Orchestration 레이어에서 트랜잭션/조립 책임)
 */
@Component
@RequiredArgsConstructor
public class UserOnlineStatusOrchestrator {
    private final UserOnlineStatusPort userOnlineStatusPort;

    public void setUserOnline(Long userId) {
        userOnlineStatusPort.setUserOnline(userId);
    }

    public void setUserOffline(Long userId) {
        userOnlineStatusPort.setUserOffline(userId);
    }

    public boolean isUserOnline(Long userId) {
        return userOnlineStatusPort.isUserOnline(userId);
    }

    public void setUserFocusRoom(Long userId, Long roomId) {
        userOnlineStatusPort.setUserFocusRoom(userId, roomId);
    }

    public void removeUserFocusRoom(Long userId) {
        userOnlineStatusPort.removeUserFocusRoom(userId);
    }

    public Long getUserFocusRoom(Long userId) {
        return userOnlineStatusPort.getUserFocusRoom(userId);
    }
}

