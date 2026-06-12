package com.back.catchmate.user.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.user.domain.model.Block;

import java.util.List;
import java.util.Optional;

public interface BlockRepository {
    Block save(Block block);

    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Page<Block> findAllByBlockerId(Long blockerId, Pageable pageable);

    List<Long> findAllBlockedUserIdsByBlockerId(Long blockerId);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void delete(Block block);
}
