package com.back.catchmate.auth.domain.exception;

import com.back.catchmate.auth.domain.AuthErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class InvalidSignupTokenException extends BusinessException {
    public InvalidSignupTokenException() {
        super(AuthErrorCode.INVALID_SIGNUP_TOKEN);
    }
}
