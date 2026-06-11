package com.back.catchmate.oauth.application.port.out;

import com.back.catchmate.oauth.domain.model.OAuthUserInfo;
import com.back.catchmate.user.domain.enums.Provider;

public interface OAuthClient {
    Provider supports();

    OAuthUserInfo exchange(String code);

    String buildAuthorizeUrl(String state);
}
