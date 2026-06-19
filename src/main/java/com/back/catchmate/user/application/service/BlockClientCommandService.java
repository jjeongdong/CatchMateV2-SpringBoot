package com.back.catchmate.user.application.service;

import com.back.catchmate.user.application.dto.response.BlockActionResponse;
import com.back.catchmate.user.application.event.UserBlockedEvent;
import com.back.catchmate.user.application.port.in.BlockClientCommandUseCase;
import com.back.catchmate.user.application.port.out.persistence.BlockRepository;
import com.back.catchmate.user.domain.model.Block;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BlockClientCommandService implements BlockClientCommandUseCase {
    private final BlockRepository blockRepository;
    private final BlockReader blockReader;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public BlockActionResponse createBlock(Long blockerId, Long blockedId) {
        blockReader.checkAlreadyBlocked(blockerId, blockedId);

        Block block = Block.createBlock(blockerId, blockedId);
        blockRepository.save(block);

        applicationEventPublisher.publishEvent(UserBlockedEvent.of(blockerId, blockedId));
        return BlockActionResponse.of(blockedId, "유저를 차단했습니다.");
    }

    @Override
    public BlockActionResponse deleteBlock(Long blockerId, Long blockedId) {
        Block block = blockReader.getBlock(blockerId, blockedId);

        blockRepository.delete(block);
        return BlockActionResponse.of(blockedId, "차단을 해제했습니다.");
    }
}
