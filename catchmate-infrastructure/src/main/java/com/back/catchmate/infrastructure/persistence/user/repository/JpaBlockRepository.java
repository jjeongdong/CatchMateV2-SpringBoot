package com.back.catchmate.infrastructure.persistence.user.repository;

import com.back.catchmate.infrastructure.persistence.user.entity.BlockEntity;
import com.back.catchmate.infrastructure.persistence.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JpaBlockRepository extends JpaRepository<BlockEntity, Long> {
    boolean existsByBlockerAndBlocked(UserEntity blocker, UserEntity blocked);
    Optional<BlockEntity> findByBlockerAndBlocked(UserEntity blocker, UserEntity blocked);

    @Query("SELECT b FROM BlockEntity b JOIN FETCH b.blocked WHERE b.blocker.id = :blockerId")
    Page<BlockEntity> findAllByBlockerId(Long blockerId, Pageable pageable);

    List<Long> findAllBlockedUserIdsByBlocker(UserEntity blocker);
}
