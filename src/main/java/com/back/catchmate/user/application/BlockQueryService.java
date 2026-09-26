package com.back.catchmate.user.application;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.dto.result.BlockResult;
import com.back.catchmate.user.domain.Block;
import com.back.catchmate.user.domain.BlockRepository;
import com.back.catchmate.user.domain.User;
import com.back.catchmate.user.domain.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockQueryService {
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public OffsetPageResult<BlockResult> getBlocks(Long blockerId, int page, int size) {
        List<Block> blocks = blockRepository.findAllByBlockerId(blockerId, (long) page * size, size);
        long totalElements = blockRepository.countByBlockerId(blockerId);
        Map<Long, User> blockedUserById = blocks.isEmpty()
                ? Map.of()
                : userRepository
                        .findAllByIds(blocks.stream().map(Block::getBlockedId).toList())
                        .stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        List<BlockResult> content = blocks.stream()
                .map(block -> BlockResult.of(block, blockedUserById.get(block.getBlockedId())))
                .toList();
        return OffsetPageResult.of(content, page, size, totalElements);
    }
}
