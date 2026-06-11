package com.back.catchmate.domain.user.repository;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.user.model.Block;
import com.back.catchmate.domain.user.model.User;

import java.util.List;
import java.util.Optional;

public interface BlockRepository {
    Block save(Block block);

    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);

    DomainPage<Block> findAllByBlockerId(Long blockerId, DomainPageable pageable);

    List<Long> findAllBlockedUserIdsByBlocker(User user);

    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    void delete(Block block);
}
