package com.back.catchmate.user.application.service;

import com.back.catchmate.user.application.port.in.UserOnlineStatusCommandUseCase;
import com.back.catchmate.user.application.port.out.external.UserOnlineStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserOnlineStatusCommandService implements UserOnlineStatusCommandUseCase {
    private final UserOnlineStatusPort userOnlineStatusPort;

    @Override
    public void setUserOnline(Long userId) {
        userOnlineStatusPort.setUserOnline(userId);
    }

    @Override
    public void setUserOffline(Long userId) {
        userOnlineStatusPort.setUserOffline(userId);
    }

    @Override
    public void setUserFocusRoom(Long userId, Long roomId) {
        userOnlineStatusPort.setUserFocusRoom(userId, roomId);
    }

    @Override
    public void removeUserFocusRoom(Long userId) {
        userOnlineStatusPort.removeUserFocusRoom(userId);
    }
}
