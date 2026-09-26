package com.back.catchmate.board.domain.exception;

import com.back.catchmate.board.domain.BoardErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class BoardCursorInvalidException extends BusinessException {
    public BoardCursorInvalidException() {
        super(BoardErrorCode.BOARD_CURSOR_INVALID);
    }
}
