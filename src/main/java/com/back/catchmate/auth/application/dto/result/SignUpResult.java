package com.back.catchmate.auth.application.dto.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

// refresh token 은 HttpOnly 쿠키로만 내보내야 해서 응답 본문에서 뺀다.
public record SignUpResult(Long userId, String accessToken, LocalDateTime createdAt, @JsonIgnore String refreshToken) {}
