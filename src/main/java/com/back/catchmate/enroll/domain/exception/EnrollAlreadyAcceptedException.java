package com.back.catchmate.enroll.domain.exception;

import com.back.catchmate.enroll.domain.EnrollErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class EnrollAlreadyAcceptedException extends BusinessException {
    public EnrollAlreadyAcceptedException() {
        super(EnrollErrorCode.ENROLL_ALREADY_ACCEPTED);
    }
}
