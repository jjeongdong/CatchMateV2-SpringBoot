package com.back.catchmate.user.application.port.in;

public interface UserOnlineStatusCommandUseCase {
    void setUserOnline(Long userId);

    void setUserOffline(Long userId);

    void setUserFocusRoom(Long userId, Long roomId);

    void removeUserFocusRoom(Long userId);
}
