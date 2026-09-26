package com.back.catchmate.user.fixture;

import com.back.catchmate.user.domain.Block;
import org.springframework.test.util.ReflectionTestUtils;

public final class BlockFixture {

    private BlockFixture() {}

    public static Block block(Long blockId, Long blockerId, Long blockedId) {
        Block block = Block.create(blockerId, blockedId);
        ReflectionTestUtils.setField(block, "id", blockId);
        return block;
    }
}
