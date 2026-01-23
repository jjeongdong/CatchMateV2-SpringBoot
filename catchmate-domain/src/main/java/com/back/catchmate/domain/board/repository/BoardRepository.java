package com.back.catchmate.domain.board.repository;

import com.back.catchmate.domain.board.model.Board;

import java.util.Optional;

public interface BoardRepository {
    Board save(Board board);
    Optional<Board> findById(Long id);
    Optional<Board> findFirstByUserIdAndIsCompletedFalse(Long userId);
    void delete(Board board);
}
