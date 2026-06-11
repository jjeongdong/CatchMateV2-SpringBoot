package com.back.catchmate.user.application.port.in;

public interface UserOnlineStatusUseCase {
    void setUserOnline(Long userId);
    void setUserOffline(Long userId);
    boolean isUserOnline(Long userId);
    void setUserFocusRoom(Long userId, Long roomId);
    void removeUserFocusRoom(Long userId);
    Long getUserFocusRoom(Long userId);
}
