package com.back.catchmate.oauth.application.port.out.external;

import com.back.catchmate.oauth.domain.model.OAuthUserInfo;
import com.back.catchmate.oauth.domain.enums.Provider;

public interface OAuthClient {
    Provider supports();

    OAuthUserInfo exchange(String code);

    String buildAuthorizeUrl(String state);
}
