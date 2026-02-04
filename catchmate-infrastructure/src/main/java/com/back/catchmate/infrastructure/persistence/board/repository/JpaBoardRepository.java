package com.back.catchmate.infrastructure.persistence.board.repository;

import com.back.catchmate.infrastructure.persistence.board.entity.BoardEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaBoardRepository extends JpaRepository<BoardEntity, Long> {
    Optional<BoardEntity> findByIdAndCompletedTrue(Long id);

    Optional<BoardEntity> findFirstByUserIdAndCompletedFalse(Long userId);

    Page<BoardEntity> findAllByUserId(Long userId, Pageable pageable);

    Page<BoardEntity> findAllByCompletedTrue(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BoardEntity b WHERE b.id = :id")
    Optional<BoardEntity> findByIdWithPessimisticLock(@Param("id") Long id);
}
