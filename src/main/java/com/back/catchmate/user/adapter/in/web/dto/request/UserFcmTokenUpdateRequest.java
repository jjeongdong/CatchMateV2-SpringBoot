package com.back.catchmate.user.adapter.in.web.dto.request;

import com.back.catchmate.user.application.dto.command.UserFcmTokenUpdateCommand;
import jakarta.validation.constraints.NotBlank;

public record UserFcmTokenUpdateRequest(
        @NotBlank(message = "fcmToken은 필수 값입니다.") String fcmToken
) {
    public UserFcmTokenUpdateCommand toCommand() {
        return new UserFcmTokenUpdateCommand(
                fcmToken
        );
    }
}
