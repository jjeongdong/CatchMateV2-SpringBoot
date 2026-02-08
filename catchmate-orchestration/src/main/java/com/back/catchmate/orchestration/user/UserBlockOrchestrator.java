package com.back.catchmate.orchestration.user;

import com.back.catchmate.orchestration.common.PagedResponse;
import com.back.catchmate.orchestration.user.dto.response.BlockActionResponse;
import com.back.catchmate.orchestration.user.dto.response.BlockedUserResponse;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.user.model.Block;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.application.user.service.BlockService;
import com.back.catchmate.application.user.service.UserService;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBlockOrchestrator {
    private final UserService userService;
    private final BlockService blockService;

    @Transactional
    public BlockActionResponse createBlock(Long blockerId, Long blockedId) {
        User blocker = userService.getUser(blockerId);
        User blocked = userService.getUser(blockedId);

        // 자기 자신 차단 방지
        if (blockerId.equals(blockedId)) {
            throw new BaseException(ErrorCode.SELF_BLOCK_FAILED);
        }

        blockService.createBlock(blocker, blocked);
        return BlockActionResponse.of(blockedId, "유저를 차단했습니다.");
    }

    @Transactional(readOnly = true)
    public PagedResponse<BlockedUserResponse> getBlockList(Long userId, int page, int size) {
        DomainPageable domainPageable = DomainPageable.of(page, size);

        DomainPage<Block> blockPage = blockService.getBlockList(userId, domainPageable);

        List<BlockedUserResponse> responses = blockPage.getContent().stream()
                .map(BlockedUserResponse::from)
                .toList();

        return new PagedResponse<>(blockPage, responses);
    }

    @Transactional
    public BlockActionResponse deleteBlock(Long blockerId, Long blockedId) {
        User blocker = userService.getUser(blockerId);
        User blocked = userService.getUser(blockedId);

        blockService.deleteBlock(blocker, blocked);
        return BlockActionResponse.of(blockedId, "차단을 해제했습니다.");
    }
}
