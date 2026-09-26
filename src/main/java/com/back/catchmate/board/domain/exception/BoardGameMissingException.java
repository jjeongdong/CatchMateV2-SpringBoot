package com.back.catchmate.board.domain.exception;

import com.back.catchmate.board.domain.BoardErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class BoardGameMissingException extends BusinessException {
    public BoardGameMissingException() {
        super(BoardErrorCode.BOARD_GAME_MISSING);
    }
}
