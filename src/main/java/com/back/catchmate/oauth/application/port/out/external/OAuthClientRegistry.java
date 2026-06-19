package com.back.catchmate.oauth.application.port.out.external;

import com.back.catchmate.oauth.domain.enums.Provider;

public interface OAuthClientRegistry {
    OAuthClient get(Provider provider);
}
