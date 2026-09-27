package com.back.catchmate.notification.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.notification.domain.NotificationErrorCode;

public class NotificationCursorInvalidException extends BusinessException {
    public NotificationCursorInvalidException() {
        super(NotificationErrorCode.NOTIFICATION_CURSOR_INVALID);
    }
}
