package com.back.catchmate.enroll.domain.exception;

import com.back.catchmate.enroll.domain.EnrollErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class EnrollAcceptInProgressException extends BusinessException {
    public EnrollAcceptInProgressException() {
        super(EnrollErrorCode.ENROLL_ACCEPT_IN_PROGRESS);
    }
}
