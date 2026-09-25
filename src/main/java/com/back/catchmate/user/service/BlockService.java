package com.back.catchmate.user.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.user.dto.response.BlockActionResponse;
import com.back.catchmate.user.dto.response.BlockedUserResponse;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.entity.Block;
import com.back.catchmate.user.event.UserBlockedEvent;
import com.back.catchmate.user.repository.BlockRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {
    private final BlockRepository blockRepository;
    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;

    // 컨트롤러용
    @Transactional
    public BlockActionResponse createBlock(Long blockerId, Long blockedId) {
        checkAlreadyBlocked(blockerId, blockedId);

        Block block = Block.createBlock(blockerId, blockedId);
        blockRepository.save(block);

        applicationEventPublisher.publishEvent(UserBlockedEvent.of(blockerId, blockedId));
        return BlockActionResponse.of(blockedId, "유저를 차단했습니다.");
    }

    @Transactional
    public BlockActionResponse deleteBlock(Long blockerId, Long blockedId) {
        Block block = getBlockOrThrow(blockerId, blockedId);

        blockRepository.deleteById(block.getId());
        return BlockActionResponse.of(blockedId, "차단을 해제했습니다.");
    }

    public PagedResponse<BlockedUserResponse> getBlockList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Block> blockPage = blockRepository.findAllByBlockerId(userId, pageable);

        if (blockPage.isEmpty()) {
            return new PagedResponse<>(blockPage, List.of());
        }

        List<Long> blockedIds =
                blockPage.getContent().stream().map(Block::getBlockedId).toList();

        Map<Long, UserSummary> blockedById = userService.getUserSummaries(blockedIds).stream()
                .collect(Collectors.toMap(UserSummary::userId, u -> u));

        List<BlockedUserResponse> responses = blockPage.getContent().stream()
                .map(block -> toBlockedUserResponse(block, blockedById.get(block.getBlockedId())))
                .toList();

        return new PagedResponse<>(blockPage, responses);
    }

    // 다른 컨텍스트용
    public List<Long> getBlockedUserIds(Long blockerId) {
        return blockRepository.findAllBlockedUserIdsByBlockerId(blockerId);
    }

    /**
     * 인자 순서 주의: 호출부(board)는 (차단 여부를 확인할 대상, 로그인 유저) 순으로 넘기고,
     * 실제 조회는 blocker = 로그인 유저, blocked = 대상으로 뒤집어 수행한다.
     */
    public boolean isUserBlocked(Long targetUserId, Long loginUserId) {
        return blockRepository.existsByBlockerIdAndBlockedId(loginUserId, targetUserId);
    }

    private BlockedUserResponse toBlockedUserResponse(Block block, UserSummary user) {
        return new BlockedUserResponse(
                block.getId(),
                user.userId(),
                user.nickName(),
                user.profileImageUrl(),
                null // blockedAt 필드는 원본 엔티티에 없거나 필요시 추가
                );
    }

    private Block getBlockOrThrow(Long blockerId, Long blockedId) {
        return blockRepository
                .findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new BaseException(ErrorCode.BLOCK_NOT_FOUND));
    }

    /**
     * 이미 차단된 유저인지 검증 규칙 처리
     */
    private void checkAlreadyBlocked(Long blockerId, Long blockedId) {
        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new BaseException(ErrorCode.ALREADY_BLOCKED);
        }
    }
}
