package com.back.catchmate.api.user.dto.request;

import com.back.catchmate.orchestration.user.dto.command.UserFcmTokenUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserFcmTokenUpdateRequest {
    @NotBlank(message = "fcmToken은 필수 값입니다.")
    private String fcmToken;

    public UserFcmTokenUpdateCommand toCommand() {
        return UserFcmTokenUpdateCommand.builder()
                .fcmToken(fcmToken)
                .build();
    }
}
