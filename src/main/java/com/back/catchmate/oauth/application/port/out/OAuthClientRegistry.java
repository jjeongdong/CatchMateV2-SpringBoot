package com.back.catchmate.oauth.application.port.out;

import com.back.catchmate.user.domain.enums.Provider;

public interface OAuthClientRegistry {
    OAuthClient get(Provider provider);
}
