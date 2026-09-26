package com.back.catchmate.enroll.domain.exception;

import com.back.catchmate.enroll.domain.EnrollErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class EnrollAlreadyPendingException extends BusinessException {
    public EnrollAlreadyPendingException() {
        super(EnrollErrorCode.ENROLL_ALREADY_PENDING);
    }
}
