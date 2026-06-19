package com.back.catchmate.chat.application.port.out.external;

public interface UserOnlineStatusCommandPort {
    void setUserOnline(Long userId);

    void setUserOffline(Long userId);

    void setUserFocusRoom(Long userId, Long roomId);

    void removeUserFocusRoom(Long userId);
}
