package com.back.catchmate.oauth.adapter.out.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {
    private ProviderProps kakao = new ProviderProps();
    private ProviderProps google = new ProviderProps();

    @Getter
    @Setter
    public static class ProviderProps {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String authorizeUrl;
        private String tokenUrl;
        private String userInfoUrl;
        private String scope;
    }
}
