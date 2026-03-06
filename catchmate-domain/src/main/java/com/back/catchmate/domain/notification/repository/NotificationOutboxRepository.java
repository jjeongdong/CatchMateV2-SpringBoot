package com.back.catchmate.domain.notification.repository;

import com.back.catchmate.domain.notification.model.NotificationOutbox;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository {
    NotificationOutbox save(NotificationOutbox outbox);
    List<NotificationOutbox> findAllPending(int maxRetryCount);
    Optional<NotificationOutbox> findById(Long id);
}
