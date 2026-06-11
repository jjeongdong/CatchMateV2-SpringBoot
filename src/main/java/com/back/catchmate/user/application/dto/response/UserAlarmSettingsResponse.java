package com.back.catchmate.user.application.dto.response;

import com.back.catchmate.user.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserAlarmSettingsResponse {
    private boolean allAlarm;
    private boolean chatAlarm;
    private boolean enrollAlarm;
    private boolean eventAlarm;

    public static UserAlarmSettingsResponse from(User user) {
        return UserAlarmSettingsResponse.builder()
                .allAlarm(user.isAllAlarmEnabled())
                .chatAlarm(user.isChatAlarmEnabled())
                .enrollAlarm(user.isEnrollAlarmEnabled())
                .eventAlarm(user.isEventAlarmEnabled())
                .build();
    }
}
