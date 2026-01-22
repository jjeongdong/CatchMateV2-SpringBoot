package com.back.catchmate.domain.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthToken {
    private final String accessToken;
    private final String refreshToken;

    public static AuthToken of(String accessToken, String refreshToken) {
        return AuthToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
