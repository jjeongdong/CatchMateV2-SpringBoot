package com.back.catchmate.enroll.domain.exception;

import com.back.catchmate.enroll.domain.EnrollErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class EnrollSelfNotAllowedException extends BusinessException {
    public EnrollSelfNotAllowedException() {
        super(EnrollErrorCode.ENROLL_SELF_NOT_ALLOWED);
    }
}
