package com.back.catchmate.oauth.application.dto.response;

public record IssuedTokenPair(
        String accessToken,
        String refreshToken
) {
}
