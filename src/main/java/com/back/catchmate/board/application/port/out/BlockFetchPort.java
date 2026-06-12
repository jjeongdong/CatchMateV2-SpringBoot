package com.back.catchmate.board.application.port.out;

import java.util.List;

public interface BlockFetchPort {
    List<Long> getBlockedUserIds(Long userId);
    boolean isUserBlocked(Long targetUserId, Long loginUserId);
}
