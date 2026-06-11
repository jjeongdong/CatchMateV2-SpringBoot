package com.back.catchmate.board.application.port.out;

import com.back.catchmate.board.domain.dto.BoardSearchCondition;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.common.page.CursorPage;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;

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

    CursorPage<Board> findAllByConditionWithCursor(BoardSearchCondition condition, int size);

    DomainPage<Board> findAllByUserId(Long userId, DomainPageable pageable);

    long count();

    void delete(Board board);
}
