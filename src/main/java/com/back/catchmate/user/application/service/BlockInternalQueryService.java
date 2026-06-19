package com.back.catchmate.user.application.service;

import com.back.catchmate.user.application.port.in.BlockInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockInternalQueryService implements BlockInternalQueryUseCase {
    private final BlockReader blockReader;

    @Override
    public List<Long> getBlockedUserIds(Long blockerId) {
        return blockReader.getBlockedUserIds(blockerId);
    }

    @Override
    public boolean isUserBlocked(Long targetUserId, Long loginUserId) {
        return blockReader.isUserBlocked(loginUserId, targetUserId);
    }
}
