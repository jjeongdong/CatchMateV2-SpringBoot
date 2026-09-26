package com.back.catchmate.auth.domain.exception;

import com.back.catchmate.auth.domain.AuthErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class OAuthStateMismatchException extends BusinessException {
    public OAuthStateMismatchException() {
        super(AuthErrorCode.OAUTH_STATE_MISMATCH);
    }
}
