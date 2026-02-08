package com.back.catchmate.orchestration.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class UserRegisterResponse {
    private Long userId;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime createdAt;

    public static UserRegisterResponse of(Long userId, String accessToken, String refreshToken, LocalDateTime createdAt) {
        return UserRegisterResponse.builder()
                .userId(userId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .createdAt(createdAt)
                .build();
    }
}
