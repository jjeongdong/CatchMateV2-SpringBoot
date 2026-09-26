package com.back.catchmate.board.domain.exception;

import com.back.catchmate.board.domain.BoardErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class BoardWriterBlockedException extends BusinessException {
    public BoardWriterBlockedException() {
        super(BoardErrorCode.BOARD_WRITER_BLOCKED);
    }
}
