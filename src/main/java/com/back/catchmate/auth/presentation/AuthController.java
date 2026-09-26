package com.back.catchmate.auth.presentation;

import com.back.catchmate.auth.application.AuthCommandService;
import com.back.catchmate.auth.application.dto.result.TokenReissueResult;
import com.back.catchmate.global.config.security.CookieFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApiDocs {
    private final AuthCommandService authCommandService;
    private final CookieFactory cookieFactory;

    // 쿠키가 없어도 서비스로 넘긴다 — 토큰 파싱이 InvalidTokenException(401)으로 막는다.
    @Override
    @PostMapping("/reissue")
    public ResponseEntity<TokenReissueResult> reissueToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        return ResponseEntity.ok(authCommandService.reissueToken(refreshToken));
    }

    // 쿠키가 없어도 로그아웃은 성공으로 응답하고 쿠키 제거 헤더를 내려준다.
    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (StringUtils.hasText(refreshToken)) {
            authCommandService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clearRefresh().toString())
                .build();
    }
}
