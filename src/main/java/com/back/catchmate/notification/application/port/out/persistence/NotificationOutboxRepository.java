package com.back.catchmate.notification.application.port.out.persistence;

import com.back.catchmate.notification.domain.model.NotificationOutbox;

import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository {
    NotificationOutbox save(NotificationOutbox outbox);

    List<NotificationOutbox> findAllPending(int maxRetryCount, int batchSize);

    List<NotificationOutbox> findAllPendingByRecipientId(Long recipientId);

    Optional<NotificationOutbox> findById(Long id);
}
