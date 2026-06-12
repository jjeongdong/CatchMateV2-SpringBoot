package com.back.catchmate.user.application.port.in;

import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.user.application.dto.response.BlockActionResponse;
import com.back.catchmate.user.application.dto.response.BlockedUserResponse;

public interface UserBlockUseCase {
    BlockActionResponse createBlock(Long blockerId, Long blockedId);
    PagedResponse<BlockedUserResponse> getBlockList(Long userId, int page, int size);
    BlockActionResponse deleteBlock(Long blockerId, Long blockedId);
}
