package com.back.catchmate.user.application.dto.response;

import com.back.catchmate.user.domain.model.Block;
import com.back.catchmate.user.domain.model.User;

import java.time.LocalDateTime;

public record BlockedUserResponse(
        Long blockId,
        Long userId,
        String nickName,
        String profileImageUrl,
        LocalDateTime blockedAt
) {
    public static BlockedUserResponse from(Block block, User blocked) {
        return new BlockedUserResponse(
                block.getId(),
                blocked.getId(),
                blocked.getNickName(),
                blocked.getProfileImageUrl(),
                null
        );
    }
}
