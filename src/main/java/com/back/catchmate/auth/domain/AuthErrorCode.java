package com.back.catchmate.auth.domain;

import com.back.catchmate.global.error.ErrorCode;
import com.back.catchmate.global.error.ErrorType;

public enum AuthErrorCode implements ErrorCode {
    INVALID_TOKEN(ErrorType.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(ErrorType.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    INVALID_SIGNUP_TOKEN(ErrorType.UNAUTHORIZED, "유효하지 않은 회원가입 토큰입니다."),
    OAUTH_PROVIDER_ERROR(ErrorType.EXTERNAL, "OAuth 공급자 통신 중 오류가 발생했습니다."),
    OAUTH_STATE_MISMATCH(ErrorType.INVALID, "OAuth state 검증에 실패했습니다."),
    UNSUPPORTED_OAUTH_PROVIDER(ErrorType.INVALID, "지원하지 않는 OAuth 공급자입니다.");

    private final ErrorType type;
    private final String message;

    AuthErrorCode(ErrorType type, String message) {
        this.type = type;
        this.message = message;
    }

    @Override
    public ErrorType type() {
        return type;
    }

    @Override
    public String message() {
        return message;
    }
}
