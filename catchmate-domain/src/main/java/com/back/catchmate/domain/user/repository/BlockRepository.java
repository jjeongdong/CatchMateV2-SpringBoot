package com.back.catchmate.domain.user.repository;

import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;
import com.back.catchmate.domain.user.model.Block;
import com.back.catchmate.domain.user.model.User;

import java.util.Optional;

public interface BlockRepository {
    Block save(Block block);
    void delete(Block block);
    boolean existsByBlockerAndBlocked(User blocker, User blocked);
    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);
    DomainPage<Block> findAllByBlocker(Long blockerId, DomainPageable pageable);
}
