package com.back.catchmate.domain.oauth.port;

import com.back.catchmate.user.enums.Provider;

public interface OAuthClientRegistry {
    OAuthClient get(Provider provider);
}
