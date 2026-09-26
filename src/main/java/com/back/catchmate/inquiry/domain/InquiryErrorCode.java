package com.back.catchmate.inquiry.domain;

import com.back.catchmate.global.error.ErrorCode;
import com.back.catchmate.global.error.ErrorType;

public enum InquiryErrorCode implements ErrorCode {
    INQUIRY_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 문의입니다."),
    INQUIRY_ALREADY_ANSWERED(ErrorType.CONFLICT, "이미 답변이 등록된 문의는 수정할 수 없습니다."),
    INQUIRY_NOT_OWNER(ErrorType.FORBIDDEN, "본인의 문의만 조회할 수 있습니다.");

    private final ErrorType type;
    private final String message;

    InquiryErrorCode(ErrorType type, String message) {
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
