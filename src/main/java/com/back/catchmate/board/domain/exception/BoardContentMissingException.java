package com.back.catchmate.board.domain.exception;

import com.back.catchmate.board.domain.BoardErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class BoardContentMissingException extends BusinessException {
    public BoardContentMissingException() {
        super(BoardErrorCode.BOARD_CONTENT_MISSING);
    }
}
