package com.back.catchmate.application.auth.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class AuthReissueResponse {
    private final String accessToken;

    public static AuthReissueResponse of(String accessToken) {
        return AuthReissueResponse.builder()
                .accessToken(accessToken)
                .build();
    }
}
