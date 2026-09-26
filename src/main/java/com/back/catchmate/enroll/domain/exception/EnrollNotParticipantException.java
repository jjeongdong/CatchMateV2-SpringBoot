package com.back.catchmate.enroll.domain.exception;

import com.back.catchmate.enroll.domain.EnrollErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class EnrollNotParticipantException extends BusinessException {
    public EnrollNotParticipantException() {
        super(EnrollErrorCode.ENROLL_NOT_PARTICIPANT);
    }
}
