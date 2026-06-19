package com.back.catchmate.oauth.application.dto.response;

import java.time.LocalDateTime;

public record CreatedUserSummary(
        Long userId,
        String authority,
        LocalDateTime createdAt
) {
}
