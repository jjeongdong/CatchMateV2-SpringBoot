package com.back.catchmate.user.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.user.application.port.out.persistence.BlockRepository;
import com.back.catchmate.user.domain.model.Block;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BlockReader {
    private final BlockRepository blockRepository;

    public Page<Block> getBlockList(Long blockerId, Pageable pageable) {
        return blockRepository.findAllByBlockerId(blockerId, pageable);
    }

    public List<Long> getBlockedUserIds(Long blockerId) {
        return blockRepository.findAllBlockedUserIdsByBlockerId(blockerId);
    }

    public boolean isUserBlocked(Long loginUserId, Long targetUserId) {
        return blockRepository.existsByBlockerIdAndBlockedId(loginUserId, targetUserId);
    }

    public Block getBlock(Long blockerId, Long blockedId) {
        return blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new BaseException(ErrorCode.BLOCK_NOT_FOUND));
    }

    /**
     * 이미 차단된 유저인지 검증 규칙 처리
     */
    public void checkAlreadyBlocked(Long blockerId, Long blockedId) {
        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new BaseException(ErrorCode.ALREADY_BLOCKED);
        }
    }
}
