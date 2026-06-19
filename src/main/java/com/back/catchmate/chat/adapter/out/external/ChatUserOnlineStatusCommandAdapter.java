package com.back.catchmate.chat.adapter.out.external;

import com.back.catchmate.chat.application.port.out.external.UserOnlineStatusCommandPort;
import com.back.catchmate.user.application.port.in.UserOnlineStatusCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatUserOnlineStatusCommandAdapter implements UserOnlineStatusCommandPort {
    private final UserOnlineStatusCommandUseCase userOnlineStatusCommandUseCase;

    @Override
    public void setUserOnline(Long userId) {
        userOnlineStatusCommandUseCase.setUserOnline(userId);
    }

    @Override
    public void setUserOffline(Long userId) {
        userOnlineStatusCommandUseCase.setUserOffline(userId);
    }

    @Override
    public void setUserFocusRoom(Long userId, Long roomId) {
        userOnlineStatusCommandUseCase.setUserFocusRoom(userId, roomId);
    }

    @Override
    public void removeUserFocusRoom(Long userId) {
        userOnlineStatusCommandUseCase.removeUserFocusRoom(userId);
    }
}
