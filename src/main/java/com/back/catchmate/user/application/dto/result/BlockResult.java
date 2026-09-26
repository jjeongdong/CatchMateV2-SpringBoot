package com.back.catchmate.user.application.dto.result;

import com.back.catchmate.user.domain.Block;
import com.back.catchmate.user.domain.User;
import java.time.LocalDateTime;

public record BlockResult(Long blockId, Long userId, String nickName, String profileImageUrl, LocalDateTime blockedAt) {

    // 차단된 유저가 탈퇴(소프트 삭제)하면 조회되지 않는다. 목록 전체를 실패시키지 않고 그 유저 정보만 비운다.
    public static BlockResult of(Block block, User blockedUser) {
        return new BlockResult(
                block.getId(),
                block.getBlockedId(),
                blockedUser != null ? blockedUser.getNickName() : null,
                blockedUser != null ? blockedUser.getProfileImageUrl() : null,
                block.getCreatedAt());
    }
}
