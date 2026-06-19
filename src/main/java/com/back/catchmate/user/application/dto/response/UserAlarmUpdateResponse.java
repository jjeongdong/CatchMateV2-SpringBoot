package com.back.catchmate.user.application.dto.response;

import com.back.catchmate.user.domain.model.UserAlarmType;

public record UserAlarmUpdateResponse(
        UserAlarmType alarmType,
        boolean enabled
) {
    public static UserAlarmUpdateResponse of(UserAlarmType alarmType, boolean enabled) {
        return new UserAlarmUpdateResponse(alarmType, enabled);
    }
}
