package com.back.catchmate.application.user;

import com.back.catchmate.domain.user.port.UserOnlineStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 사용자 온라인 상태 관리 UseCase
 */
@Service
@RequiredArgsConstructor
public class UserOnlineStatusUseCase {
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
