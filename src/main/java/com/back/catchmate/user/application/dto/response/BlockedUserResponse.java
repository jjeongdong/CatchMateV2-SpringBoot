package com.back.catchmate.user.application.dto.response;

import java.time.LocalDateTime;

public record BlockedUserResponse(
        Long blockId,
        Long userId,
        String nickName,
        String profileImageUrl,
        LocalDateTime blockedAt
) {
}
