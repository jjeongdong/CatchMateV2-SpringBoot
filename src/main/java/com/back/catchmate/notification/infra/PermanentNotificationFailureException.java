package com.back.catchmate.notification.infra;

public class PermanentNotificationFailureException extends RuntimeException {
    public PermanentNotificationFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
