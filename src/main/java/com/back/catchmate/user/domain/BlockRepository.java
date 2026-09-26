package com.back.catchmate.user.domain;

import java.util.List;

public interface BlockRepository {
    /** 이미 같은 차단이 있으면 BlockAlreadyExistsException. */
    Block save(Block block);

    Block getByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void delete(Block block);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<Long> findBlockedIdsByBlockerId(Long blockerId);

    /** 최신 차단순. */
    List<Block> findAllByBlockerId(Long blockerId, long offset, int limit);

    long countByBlockerId(Long blockerId);
}
