package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.BlockFetchPort;
import com.back.catchmate.user.application.service.BlockService;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardBlockFetchAdapter implements BlockFetchPort {
    private final BlockService blockService;

    @Override
    public List<Long> getBlockedUserIds(User user) {
        return blockService.getBlockedUserIds(user);
    }

    @Override
    public boolean isUserBlocked(User targetUser, User loginUser) {
        return blockService.isUserBlocked(targetUser, loginUser);
    }
}
