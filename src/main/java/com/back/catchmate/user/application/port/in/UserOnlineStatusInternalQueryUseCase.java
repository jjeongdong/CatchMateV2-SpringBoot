package com.back.catchmate.user.application.port.in;

import java.util.List;
import java.util.Map;

public interface UserOnlineStatusInternalQueryUseCase {
    boolean isUserOnline(Long userId);

    Long getUserFocusRoom(Long userId);

    Map<Long, Long> getUserFocusRooms(List<Long> userIds);
}
