package com.back.catchmate.user.application.port.out.external;

import java.util.List;
import java.util.Map;

public interface UserOnlineStatusPort {
    void setUserOnline(Long userId);

    void setUserOffline(Long userId);

    boolean isUserOnline(Long userId);

    void setUserFocusRoom(Long userId, Long roomId);

    void removeUserFocusRoom(Long userId);

    Long getUserFocusRoom(Long userId);

    /**
     * 여러 사용자의 포커스 방을 한 번에 조회한다. 포커스 중인 방이 없는 사용자는 결과에 포함되지 않는다.
     */
    Map<Long, Long> getUserFocusRooms(List<Long> userIds);
}
