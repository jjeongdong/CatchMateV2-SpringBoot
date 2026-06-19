package com.back.catchmate.user.application.service;

import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.user.application.dto.response.BlockedUserResponse;
import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import com.back.catchmate.user.application.port.in.BlockClientQueryUseCase;
import com.back.catchmate.user.application.port.in.UserInternalQueryUseCase;
import com.back.catchmate.user.domain.model.Block;
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
public class BlockClientQueryService implements BlockClientQueryUseCase {
    private final BlockReader blockReader;
    private final UserInternalQueryUseCase userInternalQueryUseCase;

    @Override
    public PagedResponse<BlockedUserResponse> getBlockList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Block> blockPage = blockReader.getBlockList(userId, pageable);

        if (blockPage.isEmpty()) {
            return new PagedResponse<>(blockPage, List.of());
        }

        List<Long> blockedIds = blockPage.getContent().stream().map(Block::getBlockedId).toList();

        Map<Long, UserInternalResponse> blockedById = userInternalQueryUseCase.getUsers(blockedIds).stream()
                .collect(Collectors.toMap(UserInternalResponse::userId, u -> u));

        List<BlockedUserResponse> responses = blockPage.getContent().stream()
                .map(block -> toBlockedUserResponse(block, blockedById.get(block.getBlockedId())))
                .toList();

        return new PagedResponse<>(blockPage, responses);
    }

    private BlockedUserResponse toBlockedUserResponse(Block block, UserInternalResponse user) {
        return new BlockedUserResponse(
                block.getId(),
                user.userId(),
                user.nickName(),
                user.profileImageUrl(),
                null // blockedAt 필드는 원본 엔티티에 없거나 필요시 추가
        );
    }
}
