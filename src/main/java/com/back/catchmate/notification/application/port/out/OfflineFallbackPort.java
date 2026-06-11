package com.back.catchmate.notification.application.port.out;

import com.back.catchmate.notification.domain.model.NotificationOutbox;

public interface OfflineFallbackPort {
    void dispatchIfOffline(Long userId, NotificationOutbox outbox);
}
