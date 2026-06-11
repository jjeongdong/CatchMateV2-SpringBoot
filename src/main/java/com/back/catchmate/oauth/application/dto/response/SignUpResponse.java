package com.back.catchmate.oauth.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class SignUpResponse {
    private Long userId;
    private String accessToken;
    private LocalDateTime createdAt;

    public static SignUpResponse of(Long userId, String accessToken, LocalDateTime createdAt) {
        return SignUpResponse.builder()
                .userId(userId)
                .accessToken(accessToken)
                .createdAt(createdAt)
                .build();
    }
}
