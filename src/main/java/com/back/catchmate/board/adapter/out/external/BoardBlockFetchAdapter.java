package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.BlockFetchPort;
import com.back.catchmate.user.application.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardBlockFetchAdapter implements BlockFetchPort {
    private final BlockService blockService;

    @Override
    public List<Long> getBlockedUserIds(Long userId) {
        return blockService.getBlockedUserIds(userId);
    }

    @Override
    public boolean isUserBlocked(Long targetUserId, Long loginUserId) {
        return blockService.isUserBlocked(targetUserId, loginUserId);
    }
}
