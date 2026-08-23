package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.notification.application.port.out.dto.NotificationUserInfo;
import com.back.catchmate.notification.application.port.out.external.UserFetchPort;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationUserFetchAdapter implements UserFetchPort {
    private final UserService userService;

    @Override
    public NotificationUserInfo getUser(Long userId) {
        return fromInternalResponse(userService.getUserSummary(userId));
    }

    @Override
    public List<NotificationUserInfo> getUsers(List<Long> userIds) {
        return userService.getUserSummaries(userIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    @Override
    public List<NotificationUserInfo> getEventAlarmEnabledUsers() {
        return userService.getEventAlarmEnabledUserSummaries().stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    private NotificationUserInfo fromInternalResponse(UserSummary response) {
        return new NotificationUserInfo(
                response.userId(),
                response.nickName(),
                response.profileImageUrl(),
                response.fcmToken(),
                response.chatAlarmEnabled(),
                response.enrollAlarmEnabled(),
                response.eventAlarmEnabled()
        );
    }
}
