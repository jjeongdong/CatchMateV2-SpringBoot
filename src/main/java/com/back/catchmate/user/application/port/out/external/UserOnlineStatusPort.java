package com.back.catchmate.user.application.port.out.external;

public interface UserOnlineStatusPort {
    void setUserOnline(Long userId);

    void setUserOffline(Long userId);

    boolean isUserOnline(Long userId);

    void setUserFocusRoom(Long userId, Long roomId);

    void removeUserFocusRoom(Long userId);

    Long getUserFocusRoom(Long userId);
}
