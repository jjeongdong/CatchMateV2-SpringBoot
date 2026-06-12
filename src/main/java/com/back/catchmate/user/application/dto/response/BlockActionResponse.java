package com.back.catchmate.user.application.dto.response;


public record BlockActionResponse(
        Long targetUserId,
        String message
) {
    public static BlockActionResponse of(Long targetUserId, String message) {
        return new BlockActionResponse(targetUserId, message);
    }
}
