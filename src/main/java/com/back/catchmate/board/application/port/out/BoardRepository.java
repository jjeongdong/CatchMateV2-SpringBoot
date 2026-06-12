package com.back.catchmate.board.application.port.out;

import com.back.catchmate.board.domain.dto.BoardSearchCondition;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.common.response.CursorPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BoardRepository {
    Board save(Board board);

    Optional<Board> findById(Long id);

    List<Board> findAllByIds(List<Long> ids);

    /**
     * 동시성 제어가 필요한 경우(예: 신청 수락 시 인원 증가) Board를 비관적 락으로 조회합니다.
     */
    Optional<Board> findByIdWithLock(Long id);

    Optional<Board> findCompletedById(Long id);

    Optional<Board> findTempBoardByUserId(Long userId);

    Page<Board> findAll(Pageable pageable);

    Page<Board> findAllByCondition(BoardSearchCondition condition, Pageable pageable);

    CursorPage<Board> findAllByConditionWithCursor(BoardSearchCondition condition, int size);

    Page<Board> findAllByUserId(Long userId, Pageable pageable);

    long count();

    void delete(Board board);
}
