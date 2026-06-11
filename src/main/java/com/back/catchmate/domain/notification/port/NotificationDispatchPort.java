package com.back.catchmate.domain.notification.port;

import com.back.catchmate.domain.notification.model.NotificationOutbox;

public interface NotificationDispatchPort {
    void dispatchIfOffline(Long userId, NotificationOutbox outbox);
}
