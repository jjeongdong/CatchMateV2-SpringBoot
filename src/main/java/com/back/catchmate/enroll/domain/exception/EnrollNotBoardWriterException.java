package com.back.catchmate.enroll.domain.exception;

import com.back.catchmate.enroll.domain.EnrollErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class EnrollNotBoardWriterException extends BusinessException {
    public EnrollNotBoardWriterException() {
        super(EnrollErrorCode.ENROLL_NOT_BOARD_WRITER);
    }
}
