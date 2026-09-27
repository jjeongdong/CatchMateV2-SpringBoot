package com.back.catchmate.notification.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.notification.domain.NotificationErrorCode;

public class NotificationOutboxSaveFailedException extends BusinessException {
    // 서버 원인(500)이라 전역 핸들러가 원인까지 로그로 남기도록 cause 를 보존한다.
    public NotificationOutboxSaveFailedException(Throwable cause) {
        super(NotificationErrorCode.NOTIFICATION_OUTBOX_SAVE_FAILED);
        initCause(cause);
    }
}
