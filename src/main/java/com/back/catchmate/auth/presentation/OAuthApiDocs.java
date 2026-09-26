package com.back.catchmate.auth.presentation;

import com.back.catchmate.auth.application.dto.result.SignUpResult;
import com.back.catchmate.auth.presentation.dto.request.SignUpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "[인증] OAuth 로그인 API")
public interface OAuthApiDocs {

    @Operation(summary = "회원가입 완료", description = "OAuth 콜백 단계에서 발급된 signupToken으로 회원가입을 완료하고 JWT를 발급합니다.")
    ResponseEntity<SignUpResult> signUp(SignUpRequest request);

    @Operation(summary = "OAuth 로그인 시작", description = "지정된 provider의 인증 화면으로 redirect 합니다.")
    ResponseEntity<Void> authorize(String provider, @Parameter(hidden = true) HttpServletResponse response);

    @Operation(summary = "OAuth 콜백 처리", description = "공급자로부터 받은 code를 검증하고 토큰을 발급합니다.")
    ResponseEntity<Void> callback(
            String provider,
            String code,
            String state,
            String error,
            String errorDescription,
            String stateCookie,
            @Parameter(hidden = true) HttpServletResponse response);
}
