package com.back.catchmate.user.application.dto.result;

import com.back.catchmate.user.domain.Block;

public record BlockCreateResult(Long blockId, Long blockedUserId) {
    public static BlockCreateResult from(Block block) {
        return new BlockCreateResult(block.getId(), block.getBlockedId());
    }
}
