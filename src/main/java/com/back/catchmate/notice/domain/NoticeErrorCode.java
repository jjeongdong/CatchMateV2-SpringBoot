package com.back.catchmate.notice.domain;

import com.back.catchmate.global.error.ErrorCode;
import com.back.catchmate.global.error.ErrorType;

public enum NoticeErrorCode implements ErrorCode {
    NOTICE_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 공지입니다.");

    private final ErrorType type;
    private final String message;

    NoticeErrorCode(ErrorType type, String message) {
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
