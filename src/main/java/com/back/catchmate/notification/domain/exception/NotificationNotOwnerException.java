package com.back.catchmate.notification.domain.exception;

import com.back.catchmate.global.error.BusinessException;
import com.back.catchmate.notification.domain.NotificationErrorCode;

public class NotificationNotOwnerException extends BusinessException {
    public NotificationNotOwnerException() {
        super(NotificationErrorCode.NOTIFICATION_NOT_OWNER);
    }
}
