package com.back.catchmate.enroll.domain.exception;

import com.back.catchmate.enroll.domain.EnrollErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class EnrollNotApplicantException extends BusinessException {
    public EnrollNotApplicantException() {
        super(EnrollErrorCode.ENROLL_NOT_APPLICANT);
    }
}
