package com.back.catchmate.api.auth.controller;

import com.back.catchmate.api.auth.dto.AuthLoginRequest;
import com.back.catchmate.application.auth.AuthUseCase;
import com.back.catchmate.application.auth.dto.response.AuthLoginResponse;
import com.back.catchmate.application.auth.dto.response.AuthReissueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자] 로그인 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    @Operation(summary = "로그인 & 회원가입 API", description = "회원가입 & 로그인을 통해 토큰을 발급하는 API 입니다.")
    public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(authUseCase.login(request.toCommand()));
    }

    @PostMapping("/reissue")
    @Operation(summary = "엑세스 토큰 재발급 API", description = "엑세스 토큰을 재발급하는 API 입니다.")
    public ResponseEntity<AuthReissueResponse> reissue(@RequestHeader("RefreshToken") String refreshToken) {
        return ResponseEntity.ok(authUseCase.reissue(refreshToken));
    }
}
