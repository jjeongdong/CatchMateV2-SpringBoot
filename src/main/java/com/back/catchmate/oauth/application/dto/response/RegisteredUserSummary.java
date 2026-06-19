package com.back.catchmate.oauth.application.dto.response;

public record RegisteredUserSummary(
        Long userId,
        String authority
) {
}
