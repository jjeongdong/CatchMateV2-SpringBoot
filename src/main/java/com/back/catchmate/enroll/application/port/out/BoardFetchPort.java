package com.back.catchmate.enroll.application.port.out;

import com.back.catchmate.board.application.dto.command.BoardUpdateCommand;
import com.back.catchmate.board.application.dto.response.BoardDetailResponse;
import com.back.catchmate.board.application.dto.response.BoardUpdateResponse;
import com.back.catchmate.board.domain.model.Board;

public interface BoardFetchPort {
    BoardDetailResponse getBoard(Long userId, Long boardId);
    Board getBoard(Long boardId);
    Board getBoardWithLock(Long boardId);
    Board getCompletedBoard(Long boardId);
    BoardUpdateResponse updateBoard(Long userId, Long boardId, BoardUpdateCommand command);
    void updateBoard(Board board);
}
