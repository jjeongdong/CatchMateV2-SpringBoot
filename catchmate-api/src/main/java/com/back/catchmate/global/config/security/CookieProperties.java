package com.back.catchmate.global.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cookie")
public class CookieProperties {
    private Refresh refresh = new Refresh();
    private OAuthState oauthState = new OAuthState();

    @Getter
    @Setter
    public static class Refresh {
        private String name = "refresh_token";
        private String domain;
        private boolean secure = false;
        private String sameSite = "Strict";
        private String path = "/api/auth";
        private long maxAgeMs = 2592000000L;
    }

    @Getter
    @Setter
    public static class OAuthState {
        private String name = "oauth_state";
        private String domain;
        private boolean secure = false;
        private String sameSite = "Lax";
        private String path = "/api/oauth";
        private long maxAgeSeconds = 300;
    }
}
