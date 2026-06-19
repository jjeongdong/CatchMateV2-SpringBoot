package com.back.catchmate.user.application.port.in;

import java.util.List;

public interface BlockInternalQueryUseCase {
    List<Long> getBlockedUserIds(Long blockerId);

    boolean isUserBlocked(Long targetUserId, Long loginUserId);
}
