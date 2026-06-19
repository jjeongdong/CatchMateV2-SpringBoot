package com.back.catchmate.user.application.service;

import com.back.catchmate.user.application.port.in.UserOnlineStatusInternalQueryUseCase;
import com.back.catchmate.user.application.port.out.external.UserOnlineStatusPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserOnlineStatusInternalQueryService implements UserOnlineStatusInternalQueryUseCase {
    private final UserOnlineStatusPort userOnlineStatusPort;

    @Override
    public boolean isUserOnline(Long userId) {
        return userOnlineStatusPort.isUserOnline(userId);
    }

    @Override
    public Long getUserFocusRoom(Long userId) {
        return userOnlineStatusPort.getUserFocusRoom(userId);
    }
}
