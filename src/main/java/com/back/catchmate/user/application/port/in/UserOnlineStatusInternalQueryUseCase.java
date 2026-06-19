package com.back.catchmate.user.application.port.in;

public interface UserOnlineStatusInternalQueryUseCase {
    boolean isUserOnline(Long userId);

    Long getUserFocusRoom(Long userId);
}
