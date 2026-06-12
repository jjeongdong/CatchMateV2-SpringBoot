package com.back.catchmate.admin.application.port.out;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.common.response.CursorPagedResponse;
import com.back.catchmate.common.response.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BoardFetchPort {
    CursorPagedResponse<BoardResponse> getBoardList(Long userId, LocalDate gameDate, Integer maxPerson,
                                                           List<Long> preferredTeamIdList,
                                                           LocalDateTime lastLiftUpDate, Long lastBoardId, int size);
    Page<Board> getBoardList(Pageable pageable);
    PagedResponse<BoardResponse> getBoardListByUserId(Long targetUserId, Long loginUserId, int page, int size);
    Page<Board> getBoardListByUserId(Long userId, Pageable pageable);
    Board getCompletedBoard(Long boardId);
    long getTotalBoardCount();
}
