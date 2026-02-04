package com.back.catchmate.domain.board.repository;

import com.back.catchmate.domain.board.dto.BoardSearchCondition;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;

import java.util.Optional;

public interface BoardRepository {
    Board save(Board board);

    Optional<Board> findById(Long id);

    /**
     * 동시성 제어가 필요한 경우(예: 신청 수락 시 인원 증가) Board를 비관적 락으로 조회합니다.
     */
    Optional<Board> findByIdWithLock(Long id);

    Optional<Board> findCompletedById(Long id);

    Optional<Board> findTempBoardByUserId(Long userId);

    DomainPage<Board> findAll(DomainPageable pageable);

    DomainPage<Board> findAllByCondition(BoardSearchCondition condition, DomainPageable pageable);

    DomainPage<Board> findAllByUserId(Long userId, DomainPageable pageable);

    long count();

    void delete(Board board);
}
