package com.back.catchmate.board.repository;

import com.back.catchmate.board.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long>, BoardRepositoryCustom {
    Optional<Board> findByIdAndCompletedTrue(Long id);

    Optional<Board> findFirstByUserIdAndCompletedFalse(Long userId);

    Page<Board> findAllByUserId(Long userId, Pageable pageable);

    Page<Board> findAllByCompletedTrue(Pageable pageable);
}
