package com.back.catchmate.user.application.dto.response;

import com.back.catchmate.user.domain.model.Block;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class BlockedUserResponse {
    private Long blockId;
    private Long userId;
    private String nickName;
    private String profileImageUrl;
    private LocalDateTime blockedAt;

    public static BlockedUserResponse from(Block block) {
        return BlockedUserResponse.builder()
                .blockId(block.getId())
                .userId(block.getBlocked().getId())
                .nickName(block.getBlocked().getNickName())
                .profileImageUrl(block.getBlocked().getProfileImageUrl())
                .blockedAt(null)
                .build();
    }
}
