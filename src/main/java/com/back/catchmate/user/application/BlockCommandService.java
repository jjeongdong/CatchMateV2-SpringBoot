package com.back.catchmate.user.application;

import com.back.catchmate.user.application.dto.command.BlockCreateCommand;
import com.back.catchmate.user.application.dto.result.BlockCreateResult;
import com.back.catchmate.user.domain.Block;
import com.back.catchmate.user.domain.BlockRepository;
import com.back.catchmate.user.domain.UserRepository;
import com.back.catchmate.user.domain.event.UserBlockedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockCommandService {
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BlockCreateResult createBlock(Long blockerId, BlockCreateCommand command) {
        // 없는 유저를 가리키는 고아 차단 행이 생기지 않도록 대상이 있는지 먼저 확인한다 (없으면 404).
        userRepository.getById(command.blockedUserId());
        Block block = blockRepository.save(Block.create(blockerId, command.blockedUserId()));
        eventPublisher.publishEvent(new UserBlockedEvent(block.getBlockerId(), block.getBlockedId()));
        return BlockCreateResult.from(block);
    }

    @Transactional
    public void deleteBlock(Long blockerId, Long blockedUserId) {
        blockRepository.delete(blockRepository.getByBlockerIdAndBlockedId(blockerId, blockedUserId));
    }
}
