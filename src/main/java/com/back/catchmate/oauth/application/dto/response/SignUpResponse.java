package com.back.catchmate.oauth.application.dto.response;

import java.time.LocalDateTime;

public record SignUpResponse(
        Long userId,
        String accessToken,
        LocalDateTime createdAt
) {
    public static SignUpResponse of(Long userId, String accessToken, LocalDateTime createdAt) {
        return new SignUpResponse(userId, accessToken, createdAt);
    }
}
