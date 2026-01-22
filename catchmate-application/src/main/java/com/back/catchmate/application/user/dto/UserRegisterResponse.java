package com.back.catchmate.application.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserRegisterResponse {
    private final Long userId;
    private final String accessToken;
    private final String refreshToken;
    private final LocalDateTime createdAt;

    public static UserRegisterResponse of(Long userId, String accessToken, String refreshToken, LocalDateTime createdAt) {
        return UserRegisterResponse.builder()
                .userId(userId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .createdAt(createdAt)
                .build();
    }
}
