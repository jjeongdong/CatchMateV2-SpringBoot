package com.back.catchmate.user.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import com.back.catchmate.user.domain.enums.AlarmType;

@Getter
@Builder
@AllArgsConstructor
public class UserAlarmUpdateResponse {
    private AlarmType alarmType;
    private boolean enabled;

    public static UserAlarmUpdateResponse of(AlarmType alarmType, boolean enabled) {
        return UserAlarmUpdateResponse.builder()
                .alarmType(alarmType)
                .enabled(enabled)
                .build();
    }
}
