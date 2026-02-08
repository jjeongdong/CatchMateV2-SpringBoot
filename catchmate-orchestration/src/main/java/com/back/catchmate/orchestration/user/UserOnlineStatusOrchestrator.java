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
}
