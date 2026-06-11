package com.back.catchmate.domain.oauth.model;

import com.back.catchmate.user.enums.Provider;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupTokenClaims {
    private final Provider provider;
    private final String providerId;
    private final String email;
    private final String profileImageUrl;

    private static final String SEPARATOR = "@";

    public String getProviderIdWithProvider() {
        return providerId + SEPARATOR + provider.getProvider();
    }

    public static SignupTokenClaims from(OAuthUserInfo info) {
        return SignupTokenClaims.builder()
                .provider(info.getProvider())
                .providerId(info.getProviderId())
                .email(info.getEmail())
                .profileImageUrl(info.getProfileImageUrl())
                .build();
    }
}
