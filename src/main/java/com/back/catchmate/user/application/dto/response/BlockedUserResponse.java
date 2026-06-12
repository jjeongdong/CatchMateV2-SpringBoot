package com.back.catchmate.user.application.dto.response;

import com.back.catchmate.user.domain.model.Block;
import java.time.LocalDateTime;

public record BlockedUserResponse(
        Long blockId,
        Long userId,
        String nickName,
        String profileImageUrl,
        LocalDateTime blockedAt
) {
    public static BlockedUserResponse from(Block block) {
        return new BlockedUserResponse(
                block.getId(),
                block.getBlocked().getId(),
                block.getBlocked().getNickName(),
                block.getBlocked().getProfileImageUrl(),
                null
        );
    }
}
