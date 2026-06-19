package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.external.BlockFetchPort;
import com.back.catchmate.user.application.port.in.BlockInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardBlockFetchAdapter implements BlockFetchPort {
    private final BlockInternalQueryUseCase blockInternalQueryUseCase;

    @Override
    public List<Long> getBlockedUserIds(Long userId) {
        return blockInternalQueryUseCase.getBlockedUserIds(userId);
    }

    @Override
    public boolean isUserBlocked(Long targetUserId, Long loginUserId) {
        return blockInternalQueryUseCase.isUserBlocked(targetUserId, loginUserId);
    }
}
