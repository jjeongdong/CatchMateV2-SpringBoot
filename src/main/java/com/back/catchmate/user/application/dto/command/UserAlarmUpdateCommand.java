package com.back.catchmate.user.application.dto.command;

import com.back.catchmate.user.domain.UserAlarmType;

public record UserAlarmUpdateCommand(UserAlarmType alarmType, boolean enabled) {}
