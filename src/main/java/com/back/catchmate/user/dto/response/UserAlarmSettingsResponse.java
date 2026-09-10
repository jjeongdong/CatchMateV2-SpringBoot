package com.back.catchmate.user.dto.response;

import com.back.catchmate.user.entity.User;

public record UserAlarmSettingsResponse(
        boolean allAlarm,
        boolean chatAlarm,
        boolean enrollAlarm,
        boolean eventAlarm
) {
    public static UserAlarmSettingsResponse from(User user) {
        return new UserAlarmSettingsResponse(
                user.isAllAlarmEnabled(),
                user.isChatAlarmEnabled(),
                user.isEnrollAlarmEnabled(),
                user.isEventAlarmEnabled()
        );
    }
}
