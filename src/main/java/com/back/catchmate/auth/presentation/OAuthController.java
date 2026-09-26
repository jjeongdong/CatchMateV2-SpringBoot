package com.back.catchmate.auth.presentation;

import com.back.catchmate.auth.application.AuthCommandService;
import com.back.catchmate.auth.application.dto.command.OAuthLoginCommand;
import com.back.catchmate.auth.application.dto.result.OAuthAuthorizeResult;
import com.back.catchmate.auth.application.dto.result.OAuthLoginResult;
import com.back.catchmate.auth.application.dto.result.SignUpResult;
import com.back.catchmate.auth.domain.Provider;
import com.back.catchmate.auth.presentation.dto.request.SignUpRequest;
import com.back.catchmate.global.config.security.CookieFactory;
import com.back.catchmate.global.config.security.OAuthFrontendProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController implements OAuthApiDocs {
    private final AuthCommandService authCommandService;
    private final CookieFactory cookieFactory;
    private final OAuthFrontendProperties frontendProperties;

    @Override
    @PostMapping("/signup")
    public ResponseEntity<SignUpResult> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpResult result = authCommandService.signUp(request.toCommand());
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieFactory.refresh(result.refreshToken()).toString())
                .body(result);
    }

    @Override
    @GetMapping("/authorize/{provider}")
    public ResponseEntity<Void> authorize(@PathVariable String provider, HttpServletResponse response) {
        OAuthAuthorizeResult result = authCommandService.startOAuthLogin(Provider.of(provider));
        response.addHeader(
                HttpHeaders.SET_COOKIE, cookieFactory.oauthState(result.state()).toString());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(result.url()))
                .build();
    }

    @Override
    @GetMapping("/callback/{provider}")
    public ResponseEntity<Void> callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            @CookieValue(name = "oauth_state", required = false) String stateCookie,
            HttpServletResponse response) {
        // state 쿠키는 일회용이라 성공·실패와 무관하게 먼저 지운다 (예외 응답에도 헤더가 남는다).
        response.addHeader(
                HttpHeaders.SET_COOKIE, cookieFactory.clearOAuthState().toString());

        OAuthLoginResult result = authCommandService.completeOAuthLogin(
                new OAuthLoginCommand(Provider.of(provider), code, state, stateCookie, error, errorDescription));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(buildRedirectUrl(result, response)))
                .build();
    }

    private String buildRedirectUrl(OAuthLoginResult result, HttpServletResponse response) {
        return switch (result) {
            case OAuthLoginResult.Existing existing -> {
                response.addHeader(
                        HttpHeaders.SET_COOKIE,
                        cookieFactory.refresh(existing.refreshToken()).toString());
                String base = requireBase(frontendProperties.getSuccessRedirect(), "oauth.frontend.success-redirect");
                log.info("OAuth 콜백(기존 회원) → {} 로 redirect", base);
                yield base + "?access_token=" + URLEncoder.encode(existing.accessToken(), StandardCharsets.UTF_8);
            }
            case OAuthLoginResult.NewUser newUser -> {
                String base = requireBase(frontendProperties.getSignupRedirect(), "oauth.frontend.signup-redirect");
                log.info("OAuth 콜백(신규 회원) → {} 로 redirect", base);
                yield base + "?signup_token=" + URLEncoder.encode(newUser.signupToken(), StandardCharsets.UTF_8);
            }
        };
    }

    // 설정 누락은 서버 잘못이라 전역 핸들러에서 500 으로 응답되게 둔다.
    private String requireBase(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            log.error("OAuth 프론트 redirect URL 미설정: {}", propertyName);
            throw new IllegalStateException("OAuth 프론트 redirect URL 미설정: " + propertyName);
        }
        return value;
    }
}
