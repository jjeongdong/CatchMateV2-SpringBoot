package com.back.catchmate.board.domain.exception;

import com.back.catchmate.board.domain.BoardErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class BoardFullException extends BusinessException {
    public BoardFullException() {
        super(BoardErrorCode.BOARD_FULL);
    }
}
