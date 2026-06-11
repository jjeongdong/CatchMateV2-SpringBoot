package com.back.catchmate.user.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.orchestration.PagedResponse;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.enroll.application.service.EnrollService;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.user.application.dto.response.BlockActionResponse;
import com.back.catchmate.user.application.dto.response.BlockedUserResponse;
import com.back.catchmate.user.application.port.in.UserBlockUseCase;
import com.back.catchmate.user.application.port.out.BlockRepository;
import com.back.catchmate.user.application.service.UserService;
import com.back.catchmate.user.domain.model.Block;
import com.back.catchmate.user.domain.model.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService implements UserBlockUseCase {

    private final UserService userService;
    private final EnrollService enrollService;

    @Transactional
    public BlockActionResponse createBlock(Long blockerId, Long blockedId) {
        User blocker = userService.getUser(blockerId);
        User blocked = userService.getUser(blockedId);

        createBlock(blocker, blocked);
        terminateAcceptedMatches(blockerId, blockedId);

        return BlockActionResponse.of(blockedId, "유저를 차단했습니다.");
    }

    private void terminateAcceptedMatches(Long blockerId, Long boardOwnerId) {
        List<Enroll> matches = enrollService.getAcceptedEnrollsBetween(blockerId, boardOwnerId);
        for (Enroll enroll : matches) {
            enrollService.deleteEnroll(enroll);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<BlockedUserResponse> getBlockList(Long userId, int page, int size) {
        DomainPageable domainPageable = DomainPageable.of(page, size);

        DomainPage<Block> blockPage = getBlockList(userId, domainPageable);

        List<BlockedUserResponse> responses = blockPage.getContent().stream()
                .map(BlockedUserResponse::from)
                .toList();

        return new PagedResponse<>(blockPage, responses);
    }

    @Transactional
    public BlockActionResponse deleteBlock(Long blockerId, Long blockedId) {
        User blocker = userService.getUser(blockerId);
        User blocked = userService.getUser(blockedId);

        deleteBlock(blocker, blocked);
        return BlockActionResponse.of(blockedId, "차단을 해제했습니다.");
    }


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
