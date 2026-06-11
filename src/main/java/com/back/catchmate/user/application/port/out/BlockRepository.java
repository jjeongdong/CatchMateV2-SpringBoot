package com.back.catchmate.user.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.user.domain.model.Block;
import com.back.catchmate.user.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface BlockRepository {
    Block save(Block block);

    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);

    Page<Block> findAllByBlockerId(Long blockerId, Pageable pageable);

    List<Long> findAllBlockedUserIdsByBlocker(User user);

    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    void delete(Block block);
}
