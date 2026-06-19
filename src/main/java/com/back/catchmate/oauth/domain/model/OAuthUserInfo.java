package com.back.catchmate.oauth.domain.model;

import com.back.catchmate.oauth.domain.enums.Provider;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthUserInfo {
    private final Provider provider;
    private final String providerId;
    private final String email;
    private final String profileImageUrl;
    private final String nickname;

    private static final String SEPARATOR = "@";

    public String getProviderIdWithProvider() {
        return providerId + SEPARATOR + provider.getProvider();
    }
}
