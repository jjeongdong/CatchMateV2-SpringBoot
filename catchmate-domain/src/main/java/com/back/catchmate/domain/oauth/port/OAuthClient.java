package com.back.catchmate.domain.oauth.port;

import com.back.catchmate.domain.oauth.model.OAuthUserInfo;
import com.back.catchmate.user.enums.Provider;

public interface OAuthClient {
    Provider supports();

    OAuthUserInfo exchange(String code);

    String buildAuthorizeUrl(String state);
}
