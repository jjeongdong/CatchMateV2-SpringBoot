package com.back.catchmate.user.dto.request;

import com.back.catchmate.user.dto.command.UserFcmTokenUpdateCommand;
import jakarta.validation.constraints.NotBlank;

public record UserFcmTokenUpdateRequest(
        @NotBlank(message = "fcmToken은 필수 값입니다.") String fcmToken
) {
    public UserFcmTokenUpdateCommand toCommand(Long userId) {
        return new UserFcmTokenUpdateCommand(
                userId,
                fcmToken
        );
    }
}
