package com.back.catchmate.board.domain.exception;

import com.back.catchmate.board.domain.BoardErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class BoardNotEditableAfterEnrollException extends BusinessException {
    public BoardNotEditableAfterEnrollException() {
        super(BoardErrorCode.BOARD_NOT_EDITABLE_AFTER_ENROLL);
    }
}
