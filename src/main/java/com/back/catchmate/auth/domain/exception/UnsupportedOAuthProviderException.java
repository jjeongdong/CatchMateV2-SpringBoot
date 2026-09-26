package com.back.catchmate.auth.domain.exception;

import com.back.catchmate.auth.domain.AuthErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class UnsupportedOAuthProviderException extends BusinessException {
    public UnsupportedOAuthProviderException() {
        super(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }
}
