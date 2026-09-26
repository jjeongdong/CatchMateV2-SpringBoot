package com.back.catchmate.board.domain.exception;

import com.back.catchmate.board.domain.BoardErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class BoardTitleMissingException extends BusinessException {
    public BoardTitleMissingException() {
        super(BoardErrorCode.BOARD_TITLE_MISSING);
    }
}
