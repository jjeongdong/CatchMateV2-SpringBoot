package com.back.catchmate.enroll.domain;

import com.back.catchmate.global.error.ErrorCode;
import com.back.catchmate.global.error.ErrorType;

public enum EnrollErrorCode implements ErrorCode {
    ENROLL_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 직관 신청입니다."),
    ENROLL_SELF_NOT_ALLOWED(ErrorType.INVALID, "자신의 게시글에는 직관 신청을 할 수 없습니다."),
    ENROLL_ALREADY_PENDING(ErrorType.CONFLICT, "이미 신청 대기 중인 게시글입니다."),
    ENROLL_ALREADY_REJECTED(ErrorType.CONFLICT, "이미 거절된 신청 내역이 있어 재신청할 수 없습니다."),
    ENROLL_ALREADY_ACCEPTED(ErrorType.CONFLICT, "이미 수락된 신청 내역이 있습니다."),
    ENROLL_NOT_BOARD_WRITER(ErrorType.FORBIDDEN, "게시글 작성자만 처리할 수 있습니다."),
    ENROLL_NOT_APPLICANT(ErrorType.FORBIDDEN, "본인의 신청만 취소할 수 있습니다."),
    ENROLL_NOT_PARTICIPANT(ErrorType.FORBIDDEN, "신청자 또는 게시글 작성자만 조회할 수 있습니다."),
    ENROLL_ACCEPT_IN_PROGRESS(ErrorType.CONFLICT, "이미 처리 중인 수락 요청입니다."),
    ENROLL_ACCEPT_CONFLICT(ErrorType.CONFLICT, "동시 요청이 많아 수락 처리에 실패했습니다. 잠시 후 다시 시도해주세요.");

    private final ErrorType type;
    private final String message;

    EnrollErrorCode(ErrorType type, String message) {
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
