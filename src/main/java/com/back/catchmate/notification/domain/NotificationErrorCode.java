package com.back.catchmate.notification.domain;

import com.back.catchmate.global.error.ErrorCode;
import com.back.catchmate.global.error.ErrorType;

public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 알림입니다."),
    NOTIFICATION_NOT_OWNER(ErrorType.FORBIDDEN, "본인의 알림만 다룰 수 있습니다."),
    NOTIFICATION_OUTBOX_SAVE_FAILED(ErrorType.INTERNAL, "알림 아웃박스 저장에 실패했습니다."),
    NOTIFICATION_CURSOR_INVALID(ErrorType.INVALID, "잘못된 커서입니다.");

    private final ErrorType type;
    private final String message;

    NotificationErrorCode(ErrorType type, String message) {
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
