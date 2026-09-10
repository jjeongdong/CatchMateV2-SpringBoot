package com.back.catchmate.oauth.infra;

import com.back.catchmate.oauth.dto.OAuthUserInfo;
import com.back.catchmate.oauth.entity.Provider;

public interface OAuthClient {
    Provider supports();

    OAuthUserInfo exchange(String code);

    String buildAuthorizeUrl(String state);
}
