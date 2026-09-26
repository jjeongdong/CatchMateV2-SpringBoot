package com.back.catchmate.user.presentation.dto.request;

import com.back.catchmate.user.application.dto.command.UserAlarmUpdateCommand;
import com.back.catchmate.user.domain.UserAlarmType;
import jakarta.validation.constraints.NotNull;

public record UserAlarmUpdateRequest(
        @NotNull(message = "alarmType은 필수 값입니다.") UserAlarmType alarmType,
        @NotNull(message = "enabled는 필수 값입니다.") Boolean enabled) {

    public UserAlarmUpdateCommand toCommand() {
        return new UserAlarmUpdateCommand(alarmType, enabled);
    }
}
