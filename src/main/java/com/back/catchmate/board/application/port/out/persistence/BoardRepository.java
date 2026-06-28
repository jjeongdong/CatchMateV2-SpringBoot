package com.back.catchmate.board.application.port.out.persistence;

import com.back.catchmate.board.domain.dto.BoardSearchCondition;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.common.response.CursorPage;
import org.springframework.data.domain.Page;
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

    CursorPage<Board> findAllByConditionWithCursor(BoardSearchCondition condition, int size);

    Page<Board> findAllByUserId(Long userId, Pageable pageable);

    long count();

    /** 임시저장(미완성 draft) 보드 폐기 — 일회성이라 물리 삭제. 완성 게시글 삭제는 save(deletedAt 세팅) 사용. */
    void deleteTempBoard(Board board);
}
