package com.back.catchmate.notification.application.port.in;

public interface OutboxDispatchUseCase {
    void sendPendingOutboxImmediately(Long recipientId);

    void processPendingNotifications();
}
