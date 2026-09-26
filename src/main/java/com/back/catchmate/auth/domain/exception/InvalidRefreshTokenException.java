package com.back.catchmate.auth.domain.exception;

import com.back.catchmate.auth.domain.AuthErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class InvalidRefreshTokenException extends BusinessException {
    public InvalidRefreshTokenException() {
        super(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }
}
