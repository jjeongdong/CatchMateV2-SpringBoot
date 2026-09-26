package com.back.catchmate.board.domain.exception;

import com.back.catchmate.board.domain.BoardErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class BoardNotWriterException extends BusinessException {
    public BoardNotWriterException() {
        super(BoardErrorCode.BOARD_NOT_WRITER);
    }
}
