package com.back.catchmate.user.application.port.out;

import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.user.domain.model.Block;
import com.back.catchmate.user.domain.model.User;

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
