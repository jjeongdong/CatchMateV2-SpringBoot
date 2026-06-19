package com.back.catchmate.user.application.port.in;

import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.user.application.dto.response.BlockedUserResponse;

public interface BlockClientQueryUseCase {
    PagedResponse<BlockedUserResponse> getBlockList(Long userId, int page, int size);
}
