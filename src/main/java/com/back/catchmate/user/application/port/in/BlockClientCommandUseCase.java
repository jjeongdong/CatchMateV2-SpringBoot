package com.back.catchmate.user.application.port.in;

import com.back.catchmate.user.application.dto.response.BlockActionResponse;

public interface BlockClientCommandUseCase {
    BlockActionResponse createBlock(Long blockerId, Long blockedId);

    BlockActionResponse deleteBlock(Long blockerId, Long blockedId);
}
