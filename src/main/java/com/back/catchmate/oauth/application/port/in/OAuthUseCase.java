package com.back.catchmate.oauth.application.port.in;

import com.back.catchmate.oauth.application.dto.command.OAuthCallbackCommand;
import com.back.catchmate.oauth.application.dto.response.AuthorizeRedirect;
import com.back.catchmate.oauth.application.dto.response.OAuthCallbackResult;
import com.back.catchmate.user.domain.enums.Provider;

public interface OAuthUseCase {
    AuthorizeRedirect buildAuthorizeRedirect(Provider provider);
    OAuthCallbackResult handleCallback(OAuthCallbackCommand command);
}
