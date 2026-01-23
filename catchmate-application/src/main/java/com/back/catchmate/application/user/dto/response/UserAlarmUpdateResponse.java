package com.back.catchmate.application.user.dto.response;

import com.back.catchmate.domain.user.model.AlarmType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserAlarmUpdateResponse {
    private AlarmType alarmType;
    private boolean isEnabled;

    public static UserAlarmUpdateResponse of(AlarmType alarmType, boolean isEnabled) {
        return UserAlarmUpdateResponse.builder()
                .alarmType(alarmType)
                .isEnabled(isEnabled)
                .build();
    }
}
