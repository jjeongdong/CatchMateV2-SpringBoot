package com.back.catchmate.orchestration.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
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
