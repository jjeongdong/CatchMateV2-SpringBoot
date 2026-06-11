package com.back.catchmate.global.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@EnableConfigurationProperties({CookieProperties.class, OAuthFrontendProperties.class})
@RequiredArgsConstructor
public class CookieFactory {
    private final CookieProperties props;

    public ResponseCookie refresh(String value) {
        CookieProperties.Refresh r = props.getRefresh();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(r.getName(), value)
                .httpOnly(true)
                .secure(r.isSecure())
                .sameSite(r.getSameSite())
                .path(r.getPath())
                .maxAge(Duration.ofMillis(r.getMaxAgeMs()));
        if (r.getDomain() != null && !r.getDomain().isBlank()) {
            builder.domain(r.getDomain());
        }
        return builder.build();
    }

    public ResponseCookie clearRefresh() {
        CookieProperties.Refresh r = props.getRefresh();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(r.getName(), "")
                .httpOnly(true)
                .secure(r.isSecure())
                .sameSite(r.getSameSite())
                .path(r.getPath())
                .maxAge(Duration.ZERO);
        if (r.getDomain() != null && !r.getDomain().isBlank()) {
            builder.domain(r.getDomain());
        }
        return builder.build();
    }

    public ResponseCookie oauthState(String value) {
        CookieProperties.OAuthState s = props.getOauthState();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(s.getName(), value)
                .httpOnly(true)
                .secure(s.isSecure())
                .sameSite(s.getSameSite())
                .path(s.getPath())
                .maxAge(Duration.ofSeconds(s.getMaxAgeSeconds()));
        if (s.getDomain() != null && !s.getDomain().isBlank()) {
            builder.domain(s.getDomain());
        }
        return builder.build();
    }

    public ResponseCookie clearOAuthState() {
        CookieProperties.OAuthState s = props.getOauthState();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(s.getName(), "")
                .httpOnly(true)
                .secure(s.isSecure())
                .sameSite(s.getSameSite())
                .path(s.getPath())
                .maxAge(Duration.ZERO);
        if (s.getDomain() != null && !s.getDomain().isBlank()) {
            builder.domain(s.getDomain());
        }
        return builder.build();
    }
}
