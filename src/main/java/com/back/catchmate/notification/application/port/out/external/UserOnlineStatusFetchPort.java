package com.back.catchmate.notification.application.port.out.external;

public interface UserOnlineStatusFetchPort {
    boolean isUserOnline(Long userId);

    Long getUserFocusRoom(Long userId);
}
