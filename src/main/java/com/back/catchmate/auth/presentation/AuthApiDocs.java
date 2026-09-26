package com.back.catchmate.auth.presentation;

import com.back.catchmate.auth.application.dto.result.TokenReissueResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "[인증] 토큰 관리 API")
public interface AuthApiDocs {

    @Operation(summary = "엑세스 토큰 재발급 API", description = "Refresh Token 쿠키로 엑세스 토큰을 재발급합니다.")
    ResponseEntity<TokenReissueResult> reissueToken(String refreshToken);

    @Operation(summary = "로그아웃 API", description = "Refresh Token을 무효화하고 쿠키를 제거합니다.")
    ResponseEntity<Void> logout(String refreshToken);
}
