package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.notification.application.port.out.external.UserOnlineStatusFetchPort;
import com.back.catchmate.user.service.UserOnlineStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationUserOnlineStatusFetchAdapter implements UserOnlineStatusFetchPort {
    private final UserOnlineStatusService userOnlineStatusService;

    @Override
    public boolean isUserOnline(Long userId) {
        return userOnlineStatusService.isUserOnline(userId);
    }

    @Override
    public Long getUserFocusRoom(Long userId) {
        return userOnlineStatusService.getUserFocusRoom(userId);
    }

    @Override
    public Map<Long, Long> getUserFocusRooms(List<Long> userIds) {
        return userOnlineStatusService.getUserFocusRooms(userIds);
    }
}
