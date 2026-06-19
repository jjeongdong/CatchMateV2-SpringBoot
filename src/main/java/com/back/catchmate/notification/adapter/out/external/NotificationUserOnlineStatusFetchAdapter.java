package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.notification.application.port.out.external.UserOnlineStatusFetchPort;
import com.back.catchmate.user.application.port.in.UserOnlineStatusInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationUserOnlineStatusFetchAdapter implements UserOnlineStatusFetchPort {
    private final UserOnlineStatusInternalQueryUseCase userOnlineStatusInternalQueryUseCase;

    @Override
    public boolean isUserOnline(Long userId) {
        return userOnlineStatusInternalQueryUseCase.isUserOnline(userId);
    }

    @Override
    public Long getUserFocusRoom(Long userId) {
        return userOnlineStatusInternalQueryUseCase.getUserFocusRoom(userId);
    }
}
