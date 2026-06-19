package com.back.catchmate.oauth.application.port.in;

import com.back.catchmate.oauth.application.dto.response.AuthorizeRedirect;
import com.back.catchmate.oauth.domain.enums.Provider;

public interface OAuthClientQueryUseCase {
    AuthorizeRedirect buildAuthorizeRedirect(Provider provider);
}
