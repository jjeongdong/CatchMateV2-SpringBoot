package com.back.catchmate.enroll.domain.exception;

import com.back.catchmate.enroll.domain.EnrollErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class EnrollAcceptConflictException extends BusinessException {
    public EnrollAcceptConflictException() {
        super(EnrollErrorCode.ENROLL_ACCEPT_CONFLICT);
    }
}
