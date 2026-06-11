package com.back.catchmate.orchestration.user;

import com.back.catchmate.orchestration.common.PagedResponse;
import com.back.catchmate.orchestration.user.dto.response.BlockActionResponse;
import com.back.catchmate.orchestration.user.dto.response.BlockedUserResponse;
import com.back.catchmate.application.enroll.service.EnrollService;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.domain.user.model.Block;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.application.user.service.BlockService;
import com.back.catchmate.application.user.service.UserService;
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
    private final EnrollService enrollService;

    @Transactional
    public BlockActionResponse createBlock(Long blockerId, Long blockedId) {
        User blocker = userService.getUser(blockerId);
        User blocked = userService.getUser(blockedId);

        blockService.createBlock(blocker, blocked);
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
