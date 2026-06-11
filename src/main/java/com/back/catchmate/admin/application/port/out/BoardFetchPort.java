package com.back.catchmate.admin.application.port.out;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.common.orchestration.CursorPagedResponse;
import com.back.catchmate.common.orchestration.PagedResponse;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BoardFetchPort {
    CursorPagedResponse<BoardResponse> getBoardList(Long userId, LocalDate gameDate, Integer maxPerson,
                                                           List<Long> preferredTeamIdList,
                                                           LocalDateTime lastLiftUpDate, Long lastBoardId, int size);
    DomainPage<Board> getBoardList(DomainPageable pageable);
    PagedResponse<BoardResponse> getBoardListByUserId(Long targetUserId, Long loginUserId, int page, int size);
    DomainPage<Board> getBoardListByUserId(Long userId, DomainPageable pageable);
    Board getCompletedBoard(Long boardId);
    long getTotalBoardCount();
}
