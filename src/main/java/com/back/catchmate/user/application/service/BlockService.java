package com.back.catchmate.user.application.service;

import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.user.domain.model.Block;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.user.application.port.out.BlockRepository;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockService {
    private final BlockRepository blockRepository;

    public void createBlock(User blocker, User blocked) {
        // 이미 차단했는지 확인
        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            throw new BaseException(ErrorCode.ALREADY_BLOCKED);
        }

        Block block = Block.createBlock(blocker, blocked);
        blockRepository.save(block);
    }

    public DomainPage<Block> getBlockList(Long blockerId, DomainPageable pageable) {
        return blockRepository.findAllByBlockerId(blockerId, pageable);
    }

    public List<Long> getBlockedUserIds(User user) {
        return blockRepository.findAllBlockedUserIdsByBlocker(user);
    }

    public boolean isUserBlocked(User targetUser, User loginUser) {
        return blockRepository.existsByBlockerAndBlocked(loginUser, targetUser);
    }

    public void deleteBlock(User blocker, User blocked) {
        Block block = blockRepository.findByBlockerAndBlocked(blocker, blocked)
                .orElseThrow(() -> new BaseException(ErrorCode.BLOCK_NOT_FOUND));

        blockRepository.delete(block);
    }
}
