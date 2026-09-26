package com.back.catchmate.auth.domain.exception;

import com.back.catchmate.auth.domain.AuthErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class OAuthProviderException extends BusinessException {
    public OAuthProviderException() {
        super(AuthErrorCode.OAUTH_PROVIDER_ERROR);
    }
}
