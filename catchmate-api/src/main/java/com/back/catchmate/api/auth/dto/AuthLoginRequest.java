package com.back.catchmate.api.auth.dto;

import com.back.catchmate.application.auth.dto.AuthLoginCommand;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AuthLoginRequest {
    @NotBlank(message = "providerId는 필수 값입니다.")
    private String providerId;

    @NotBlank(message = "provider는 필수 값입니다.")
    private String provider;

    private String fcmToken;

    private static final String SEPARATOR = "@";

    public AuthLoginCommand toCommand() {
        String providerIdWithProvider = providerId + SEPARATOR + provider;
        return AuthLoginCommand.builder()
                .providerIdWithProvider(providerIdWithProvider)
                .fcmToken(fcmToken)
                .build();
    }
}
