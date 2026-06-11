package com.back.catchmate.board.application.port.in;

import com.back.catchmate.board.application.dto.command.BoardCreateCommand;
import com.back.catchmate.board.application.dto.command.BoardUpdateCommand;
import com.back.catchmate.board.application.dto.response.BoardCreateResponse;
import com.back.catchmate.board.application.dto.response.BoardDetailResponse;
import com.back.catchmate.board.application.dto.response.BoardLiftUpResponse;
import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.application.dto.response.BoardTempDetailResponse;
import com.back.catchmate.board.application.dto.response.BoardUpdateResponse;
import com.back.catchmate.common.orchestration.CursorPagedResponse;
import com.back.catchmate.common.orchestration.PagedResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BoardUseCase {
    BoardCreateResponse createBoard(Long userId, BoardCreateCommand command);
    BoardDetailResponse getBoard(Long userId, Long boardId);
    CursorPagedResponse<BoardResponse> getBoardList(Long userId, LocalDate gameDate, Integer maxPerson,
                                                           List<Long> preferredTeamIdList,
                                                           LocalDateTime lastLiftUpDate, Long lastBoardId, int size);
    PagedResponse<BoardResponse> getBoardListByUserId(Long targetUserId, Long loginUserId, int page, int size);
    BoardTempDetailResponse getTempBoard(Long userId);
    BoardUpdateResponse updateBoard(Long userId, Long boardId, BoardUpdateCommand command);
    BoardLiftUpResponse updateLiftUpDate(Long userId, Long boardId);
    void deleteBoard(Long userId, Long boardId);
}
