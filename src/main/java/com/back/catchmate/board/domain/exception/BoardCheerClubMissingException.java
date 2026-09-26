package com.back.catchmate.board.domain.exception;

import com.back.catchmate.board.domain.BoardErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class BoardCheerClubMissingException extends BusinessException {
    public BoardCheerClubMissingException() {
        super(BoardErrorCode.BOARD_CHEER_CLUB_MISSING);
    }
}
