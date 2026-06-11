package com.back.catchmate.api.oauth.controller;

import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import com.back.catchmate.global.config.security.CookieFactory;
import com.back.catchmate.global.config.security.OAuthFrontendProperties;
import com.back.catchmate.orchestration.oauth.OAuthOrchestrator;
import com.back.catchmate.orchestration.oauth.dto.command.OAuthCallbackCommand;
import com.back.catchmate.orchestration.oauth.dto.response.AuthorizeRedirect;
import com.back.catchmate.orchestration.oauth.dto.response.OAuthCallbackResult;
import com.back.catchmate.user.enums.Provider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Tag(name = "[인증] OAuth 로그인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oauth")
public class OAuthController {
    private final OAuthOrchestrator oauthOrchestrator;
    private final CookieFactory cookieFactory;
    private final OAuthFrontendProperties frontendProperties;

    @GetMapping("/authorize/{provider}")
    @Operation(summary = "OAuth 로그인 시작", description = "지정된 provider의 인증 화면으로 redirect 합니다.")
    public ResponseEntity<Void> authorize(@PathVariable String provider, HttpServletResponse response) {
        Provider p = Provider.of(provider);
        AuthorizeRedirect redirect = oauthOrchestrator.buildAuthorizeRedirect(p);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.oauthState(redirect.state()).toString());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect.url()))
                .build();
    }

    @GetMapping("/callback/{provider}")
    @Operation(summary = "OAuth 콜백 처리", description = "공급자로부터 받은 code를 검증하고 토큰을 발급합니다.")
    public ResponseEntity<Void> callback(@PathVariable String provider,
                                         @RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error,
                                         @RequestParam(name = "error_description", required = false) String errorDescription,
                                         @CookieValue(name = "oauth_state", required = false) String stateCookie,
                                         HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearOAuthState().toString());

        if (error != null) {
            log.warn("OAuth provider returned error: provider={}, error={}, description={}",
                    provider, error, errorDescription);
            throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            log.warn("OAuth callback missing code/state: provider={}, code={}, state={}",
                    provider, code, state);
            throw new BaseException(ErrorCode.OAUTH_PROVIDER_ERROR);
        }

        Provider p = Provider.of(provider);
        OAuthCallbackResult result = oauthOrchestrator.handleCallback(OAuthCallbackCommand.builder()
                .provider(p)
                .code(code)
                .state(state)
                .stateFromCookie(stateCookie)
                .build());

        String redirectUrl = buildRedirectUrl(result, response);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    private String buildRedirectUrl(OAuthCallbackResult result, HttpServletResponse response) {
        if (result instanceof OAuthCallbackResult.Existing existing) {
            response.addHeader(HttpHeaders.SET_COOKIE,
                    cookieFactory.refresh(existing.refreshToken()).toString());
            String base = requireBase(frontendProperties.getSuccessRedirect(), "oauth.frontend.success-redirect");
            String redirect = base + "?access_token=" + URLEncoder.encode(existing.accessToken(), StandardCharsets.UTF_8);
            log.info("OAuth callback (existing user) → redirecting to {}", base);
            return redirect;
        }
        if (result instanceof OAuthCallbackResult.NewUser newUser) {
            String base = requireBase(frontendProperties.getSignupRedirect(), "oauth.frontend.signup-redirect");
            String redirect = base + "?signup_token=" + URLEncoder.encode(newUser.signupToken(), StandardCharsets.UTF_8);
            log.info("OAuth callback (new user) → redirecting to {}", base);
            return redirect;
        }
        throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private String requireBase(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            log.error("OAuth frontend redirect URL not configured: {}", propertyName);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return value;
    }
}
