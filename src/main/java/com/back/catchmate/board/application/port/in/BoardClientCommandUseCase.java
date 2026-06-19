package com.back.catchmate.board.application.port.in;

import com.back.catchmate.board.application.dto.command.BoardCreateCommand;
import com.back.catchmate.board.application.dto.command.BoardUpdateCommand;
import com.back.catchmate.board.application.dto.response.BoardCreateResponse;
import com.back.catchmate.board.application.dto.response.BoardLiftUpResponse;
import com.back.catchmate.board.application.dto.response.BoardUpdateResponse;

public interface BoardClientCommandUseCase {
    BoardCreateResponse createBoard(Long userId, BoardCreateCommand command);

    BoardUpdateResponse updateBoard(Long userId, Long boardId, BoardUpdateCommand command);

    BoardLiftUpResponse updateLiftUpDate(Long userId, Long boardId);

    void deleteBoard(Long userId, Long boardId);
}
