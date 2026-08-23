package com.back.catchmate.user.repository;

import com.back.catchmate.user.entity.Block;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Page<Block> findAllByBlockerId(Long blockerId, Pageable pageable);

    @Query("SELECT b.blockedId FROM Block b WHERE b.blockerId = :blockerId")
    List<Long> findAllBlockedUserIdsByBlockerId(@Param("blockerId") Long blockerId);
}
