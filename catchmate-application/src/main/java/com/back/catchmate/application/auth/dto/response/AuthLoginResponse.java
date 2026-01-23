package com.back.catchmate.application.auth.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthLoginResponse {
    private String accessToken;
    private String refreshToken;
    private boolean signupRequired;

    public static AuthLoginResponse of(String accessToken, String refreshToken, boolean signupRequired) {
        return AuthLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .signupRequired(signupRequired)
                .build();
    }
}
