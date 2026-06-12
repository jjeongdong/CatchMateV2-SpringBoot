package com.back.catchmate.user.application.dto.response;

import com.back.catchmate.user.domain.enums.AlarmType;

public record UserAlarmUpdateResponse(
        AlarmType alarmType,
        boolean enabled
) {
    public static UserAlarmUpdateResponse of(AlarmType alarmType, boolean enabled) {
        return new UserAlarmUpdateResponse(alarmType, enabled);
    }
}
