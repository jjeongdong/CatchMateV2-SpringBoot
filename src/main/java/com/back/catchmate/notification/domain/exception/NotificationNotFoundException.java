package com.back.catchmate.notification.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.notification.domain.NotificationErrorCode;

public class NotificationNotFoundException extends BusinessException {
    public NotificationNotFoundException() {
        super(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }
}
