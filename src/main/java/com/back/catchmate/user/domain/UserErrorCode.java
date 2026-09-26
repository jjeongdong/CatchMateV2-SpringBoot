package com.back.catchmate.user.domain;

import com.back.catchmate.global.error.ErrorCode;
import com.back.catchmate.global.error.ErrorType;

public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 사용자입니다."),
    USER_ALREADY_EXISTS(ErrorType.CONFLICT, "이미 가입된 사용자입니다."),
    BLOCK_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 차단 내역입니다."),
    BLOCK_ALREADY_EXISTS(ErrorType.CONFLICT, "해당 유저를 이미 차단했습니다."),
    BLOCK_SELF_NOT_ALLOWED(ErrorType.INVALID, "자기 자신을 차단할 수 없습니다.");

    private final ErrorType type;
    private final String message;

    UserErrorCode(ErrorType type, String message) {
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
