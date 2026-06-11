package com.back.catchmate.user.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserFcmTokenUpdateCommand {
    private String fcmToken;
}
