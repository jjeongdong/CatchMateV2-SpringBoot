package com.back.catchmate.application.user;

import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.application.user.dto.response.BlockActionResponse;
import com.back.catchmate.application.user.dto.response.BlockedUserResponse;
import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;
import com.back.catchmate.domain.user.model.Block;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.BlockService;
import com.back.catchmate.domain.user.service.UserService;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserBlockUseCase {
    private final UserService userService;
    private final BlockService blockService;

    @Transactional
    public BlockActionResponse blockUser(Long blockerId, Long blockedId) {
        User blocker = userService.getUserById(blockerId);
        User blocked = userService.getUserById(blockedId);

        // 자기 자신 차단 방지
        if (blockerId.equals(blockedId)) {
            throw new BaseException(ErrorCode.SELF_BLOCK_FAILED);
        }

        blockService.blockUser(blocker, blocked);
        return BlockActionResponse.of(blockedId, "유저를 차단했습니다.");
    }

    @Transactional
    public BlockActionResponse unblockUser(Long blockerId, Long blockedId) {
        User blocker = userService.getUserById(blockerId);
        User blocked = userService.getUserById(blockedId);

        blockService.unblockUser(blocker, blocked);
        return BlockActionResponse.of(blockedId, "차단을 해제했습니다.");
    }

    @Transactional(readOnly = true)
    public PagedResponse<BlockedUserResponse> getBlockedList(Long userId, int pageable, int size) {
        DomainPageable domainPageable = DomainPageable.of(pageable, size);

        DomainPage<Block> blockPage = blockService.getBlockedUsers(userId, domainPageable);

        List<BlockedUserResponse> responses = blockPage.getContent().stream()
                .map(BlockedUserResponse::from)
                .toList();

        return new PagedResponse<>(blockPage, responses);
    }
}
