package com.back.catchmate.application.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BlockActionResponse {
    private Long targetUserId;
    private String message;

    public static BlockActionResponse of(Long targetUserId, String message) {
        return BlockActionResponse.builder()
                .targetUserId(targetUserId)
                .message(message)
                .build();
    }
}
