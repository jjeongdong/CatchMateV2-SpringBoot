package com.back.catchmate.board.infrastructure;

import com.back.catchmate.board.domain.Board;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardJpaRepository extends JpaRepository<Board, Long> {
    Optional<Board> findByIdAndCompletedTrue(Long boardId);

    Optional<Board> findFirstByUserIdAndCompletedFalse(Long userId);

    long countByCompletedTrue();

    long countByUserId(Long userId);
}
