package com.back.catchmate.user.dto.response;

import com.back.catchmate.user.entity.UserAlarmType;

public record UserAlarmUpdateResponse(
        UserAlarmType alarmType,
        boolean enabled
) {
    public static UserAlarmUpdateResponse of(UserAlarmType alarmType, boolean enabled) {
        return new UserAlarmUpdateResponse(alarmType, enabled);
    }
}
