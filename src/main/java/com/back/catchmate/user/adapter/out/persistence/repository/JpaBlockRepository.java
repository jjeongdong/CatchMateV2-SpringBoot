package com.back.catchmate.user.adapter.out.persistence.repository;

import com.back.catchmate.user.adapter.out.persistence.entity.BlockEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaBlockRepository extends JpaRepository<BlockEntity, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<BlockEntity> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("SELECT b FROM BlockEntity b JOIN FETCH b.blocked WHERE b.blocker.id = :blockerId")
    Page<BlockEntity> findAllByBlockerId(Long blockerId, Pageable pageable);

    @Query("SELECT b.blocked.id FROM BlockEntity b WHERE b.blocker.id = :blockerId")
    List<Long> findAllBlockedUserIdsByBlockerId(@Param("blockerId") Long blockerId);
}
