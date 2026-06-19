package com.back.catchmate.board.application.port.in;

import com.back.catchmate.board.application.dto.response.BoardInternalResponse;

import java.util.List;

public interface BoardInternalQueryUseCase {
    BoardInternalResponse getBoard(Long boardId);

    List<BoardInternalResponse> getBoards(List<Long> boardIds);

    BoardInternalResponse getBoardWithLock(Long boardId);

    BoardInternalResponse getCompletedBoard(Long boardId);
}
