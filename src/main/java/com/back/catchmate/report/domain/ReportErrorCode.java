package com.back.catchmate.report.domain;

import com.back.catchmate.global.error.ErrorCode;
import com.back.catchmate.global.error.ErrorType;

public enum ReportErrorCode implements ErrorCode {
    REPORT_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 신고입니다."),
    REPORT_SELF_NOT_ALLOWED(ErrorType.INVALID, "자기 자신을 신고할 수 없습니다.");

    private final ErrorType type;
    private final String message;

    ReportErrorCode(ErrorType type, String message) {
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
