package com.back.catchmate.notification.application.port.out.external;

import com.back.catchmate.notification.application.port.out.dto.NotificationUserInfo;

import java.util.List;

public interface UserFetchPort {
    NotificationUserInfo getUser(Long userId);

    List<NotificationUserInfo> getUsers(List<Long> userIds);

    List<NotificationUserInfo> getEventAlarmEnabledUsers();
}
