package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.BoardFetchPort;
import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.application.service.BoardService;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.common.response.CursorPagedResponse;
import com.back.catchmate.common.response.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBoardFetchAdapter implements BoardFetchPort {
    private final BoardService boardService;

    @Override
    public CursorPagedResponse<BoardResponse> getBoardList(Long userId, LocalDate gameDate, Integer maxPerson,
                                                           List<Long> preferredTeamIdList,
                                                           LocalDateTime lastLiftUpDate, Long lastBoardId, int size) {
        return boardService.getBoardList(userId, gameDate, maxPerson, preferredTeamIdList, lastLiftUpDate, lastBoardId, size);
    }

    @Override
    public Page<Board> getBoardList(Pageable pageable) {
        return boardService.getBoardList(pageable);
    }

    @Override
    public PagedResponse<BoardResponse> getBoardListByUserId(Long targetUserId, Long loginUserId, int page, int size) {
        return boardService.getBoardListByUserId(targetUserId, loginUserId, page, size);
    }

    @Override
    public Page<Board> getBoardListByUserId(Long userId, Pageable pageable) {
        return boardService.getBoardListByUserId(userId, pageable);
    }

    @Override
    public Board getCompletedBoard(Long boardId) {
        return boardService.getCompletedBoard(boardId);
    }

    @Override
    public long getTotalBoardCount() {
        return boardService.getTotalBoardCount();
    }
}
