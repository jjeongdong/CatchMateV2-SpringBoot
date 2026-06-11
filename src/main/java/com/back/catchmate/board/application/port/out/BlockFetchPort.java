package com.back.catchmate.board.application.port.out;

import com.back.catchmate.user.domain.model.User;

import java.util.List;

public interface BlockFetchPort {
    List<Long> getBlockedUserIds(User user);
    boolean isUserBlocked(User targetUser, User loginUser);
}
