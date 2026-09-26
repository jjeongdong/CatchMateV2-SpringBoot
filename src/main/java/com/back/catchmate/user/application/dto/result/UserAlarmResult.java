package com.back.catchmate.user.application.dto.result;

import com.back.catchmate.user.domain.User;

public record UserAlarmResult(boolean allAlarm, boolean chatAlarm, boolean enrollAlarm, boolean eventAlarm) {
    public static UserAlarmResult from(User user) {
        return new UserAlarmResult(
                user.isAllAlarmEnabled(),
                user.isChatAlarmEnabled(),
                user.isEnrollAlarmEnabled(),
                user.isEventAlarmEnabled());
    }
}
