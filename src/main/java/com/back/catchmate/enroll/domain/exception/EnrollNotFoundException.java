package com.back.catchmate.enroll.domain.exception;

import com.back.catchmate.enroll.domain.EnrollErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class EnrollNotFoundException extends BusinessException {
    public EnrollNotFoundException() {
        super(EnrollErrorCode.ENROLL_NOT_FOUND);
    }
}
