package com.back.catchmate.user.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.user.application.dto.response.BlockActionResponse;
import com.back.catchmate.user.application.dto.response.BlockedUserResponse;
import com.back.catchmate.user.application.port.in.UserBlockUseCase;
import com.back.catchmate.user.application.port.out.BlockRepository;
import com.back.catchmate.user.application.port.out.EnrollFetchPort;
import com.back.catchmate.user.domain.model.Block;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService implements UserBlockUseCase {

    private final BlockRepository blockRepository;
    private final UserService userService;

    private final EnrollFetchPort enrollFetchPort;

    @Transactional
    public BlockActionResponse createBlock(Long blockerId, Long blockedId) {
        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new BaseException(ErrorCode.ALREADY_BLOCKED);
        }
        Block block = Block.createBlock(blockerId, blockedId);
        blockRepository.save(block);
        terminateAcceptedMatches(blockerId, blockedId);
        return BlockActionResponse.of(blockedId, "유저를 차단했습니다.");
    }

    private void terminateAcceptedMatches(Long blockerId, Long boardOwnerId) {
        List<Enroll> matches = enrollFetchPort.getAcceptedEnrollsBetween(blockerId, boardOwnerId);
        for (Enroll enroll : matches) {
            enrollFetchPort.deleteEnroll(enroll);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<BlockedUserResponse> getBlockList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Block> blockPage = getBlockList(userId, pageable);

        List<Long> blockedIds = blockPage.getContent().stream().map(Block::getBlockedId).toList();
        Map<Long, User> blockedById = userService.getUsers(blockedIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<BlockedUserResponse> responses = blockPage.getContent().stream()
                .map(b -> BlockedUserResponse.from(b, blockedById.get(b.getBlockedId())))
                .toList();

        return new PagedResponse<>(blockPage, responses);
    }

    @Transactional
    public BlockActionResponse deleteBlock(Long blockerId, Long blockedId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new BaseException(ErrorCode.BLOCK_NOT_FOUND));
        blockRepository.delete(block);
        return BlockActionResponse.of(blockedId, "차단을 해제했습니다.");
    }

    public Page<Block> getBlockList(Long blockerId, Pageable pageable) {
        return blockRepository.findAllByBlockerId(blockerId, pageable);
    }

    public List<Long> getBlockedUserIds(Long blockerId) {
        return blockRepository.findAllBlockedUserIdsByBlockerId(blockerId);
    }

    public boolean isUserBlocked(Long targetUserId, Long loginUserId) {
        return blockRepository.existsByBlockerIdAndBlockedId(loginUserId, targetUserId);
    }
}
