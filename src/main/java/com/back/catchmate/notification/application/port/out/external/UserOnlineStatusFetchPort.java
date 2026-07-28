package com.back.catchmate.notification.application.port.out.external;

import java.util.List;
import java.util.Map;

public interface UserOnlineStatusFetchPort {
    boolean isUserOnline(Long userId);

    Long getUserFocusRoom(Long userId);

    Map<Long, Long> getUserFocusRooms(List<Long> userIds);
}
