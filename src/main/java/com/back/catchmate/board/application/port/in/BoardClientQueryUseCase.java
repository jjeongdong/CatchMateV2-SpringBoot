package com.back.catchmate.board.application.port.in;

import com.back.catchmate.board.application.dto.response.BoardDetailResponse;
import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.application.dto.response.BoardTempDetailResponse;
import com.back.catchmate.common.response.CursorPagedResponse;
import com.back.catchmate.common.response.PagedResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BoardClientQueryUseCase {
    BoardDetailResponse getBoard(Long userId, Long boardId);

    BoardTempDetailResponse getTempBoard(Long userId);

    CursorPagedResponse<BoardResponse> getBoardList(Long userId, LocalDate gameDate, Integer maxPerson, List<Long> preferredTeamIdList,
                                                    LocalDateTime lastLiftUpDate, Long lastBoardId, int size);

    PagedResponse<BoardResponse> getBoardListByUserId(Long targetUserId, Long loginUserId, int page, int size);
}
