package com.back.catchmate.auth.domain.exception;

import com.back.catchmate.auth.domain.AuthErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class InvalidTokenException extends BusinessException {
    public InvalidTokenException() {
        super(AuthErrorCode.INVALID_TOKEN);
    }
}
