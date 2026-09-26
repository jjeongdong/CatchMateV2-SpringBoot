package com.back.catchmate.enroll.domain.exception;

import com.back.catchmate.enroll.domain.EnrollErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class EnrollAlreadyRejectedException extends BusinessException {
    public EnrollAlreadyRejectedException() {
        super(EnrollErrorCode.ENROLL_ALREADY_REJECTED);
    }
}
