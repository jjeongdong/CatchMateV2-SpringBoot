package com.back.catchmate.infrastructure.persistence.board.repository;

import com.back.catchmate.infrastructure.persistence.board.entity.BoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaBoardRepository extends JpaRepository<BoardEntity, Long> {
    Optional<BoardEntity> findFirstByUserIdAndCompletedFalse(Long userId);
}
