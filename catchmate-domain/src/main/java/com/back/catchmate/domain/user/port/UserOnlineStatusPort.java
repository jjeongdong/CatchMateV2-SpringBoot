package com.back.catchmate.domain.user.port;

/**
 * 사용자 온라인 상태 관리 포트
 */
public interface UserOnlineStatusPort {
    void setUserOnline(Long userId);

    void setUserOffline(Long userId);

    boolean isUserOnline(Long userId);

    void setUserFocusRoom(Long userId, Long roomId);

    void removeUserFocusRoom(Long userId);

    Long getUserFocusRoom(Long userId);
}
