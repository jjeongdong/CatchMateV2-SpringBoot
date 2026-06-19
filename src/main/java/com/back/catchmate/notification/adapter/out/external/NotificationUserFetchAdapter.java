package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.notification.application.port.out.dto.NotificationUserInfo;
import com.back.catchmate.notification.application.port.out.external.UserFetchPort;
import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import com.back.catchmate.user.application.port.in.UserInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationUserFetchAdapter implements UserFetchPort {
    private final UserInternalQueryUseCase userInternalQueryUseCase;

    @Override
    public NotificationUserInfo getUser(Long userId) {
        return fromInternalResponse(userInternalQueryUseCase.getUser(userId));
    }

    @Override
    public List<NotificationUserInfo> getUsers(List<Long> userIds) {
        return userInternalQueryUseCase.getUsers(userIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    @Override
    public List<NotificationUserInfo> getEventAlarmEnabledUsers() {
        return userInternalQueryUseCase.getEventAlarmEnabledUsers().stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    private NotificationUserInfo fromInternalResponse(UserInternalResponse response) {
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
