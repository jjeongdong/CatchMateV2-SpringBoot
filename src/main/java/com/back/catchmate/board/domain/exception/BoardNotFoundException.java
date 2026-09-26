package com.back.catchmate.board.domain.exception;

import com.back.catchmate.board.domain.BoardErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class BoardNotFoundException extends BusinessException {
    public BoardNotFoundException() {
        super(BoardErrorCode.BOARD_NOT_FOUND);
    }
}
