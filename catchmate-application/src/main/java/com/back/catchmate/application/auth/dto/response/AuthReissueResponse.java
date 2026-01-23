package com.back.catchmate.application.auth.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthReissueResponse {
    private String accessToken;

    public static AuthReissueResponse of(String accessToken) {
        return AuthReissueResponse.builder()
                .accessToken(accessToken)
                .build();
    }
}
