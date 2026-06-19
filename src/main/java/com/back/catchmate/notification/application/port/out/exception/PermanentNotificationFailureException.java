package com.back.catchmate.notification.application.port.out.exception;

public class PermanentNotificationFailureException extends RuntimeException {
    public PermanentNotificationFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
