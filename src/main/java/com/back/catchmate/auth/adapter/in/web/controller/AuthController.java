package com.back.catchmate.auth.adapter.in.web.controller;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.global.config.security.CookieFactory;
import com.back.catchmate.auth.application.port.in.AuthUseCase;
import com.back.catchmate.auth.application.dto.response.AuthReissueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[인증] 토큰 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthUseCase authOrchestrator;
    private final CookieFactory cookieFactory;

    @PostMapping("/reissue")
    @Operation(summary = "엑세스 토큰 재발급 API", description = "Refresh Token 쿠키로 엑세스 토큰을 재발급합니다.")
    public ResponseEntity<AuthReissueResponse> updateToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BaseException(ErrorCode.MISSING_REFRESH_COOKIE);
        }
        return ResponseEntity.ok(authOrchestrator.updateToken(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃 API", description = "Refresh Token을 무효화하고 쿠키를 제거합니다.")
    public ResponseEntity<Void> deleteToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authOrchestrator.deleteToken(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clearRefresh().toString())
                .build();
    }
}
