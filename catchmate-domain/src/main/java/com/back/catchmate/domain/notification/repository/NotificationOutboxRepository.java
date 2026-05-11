package com.back.catchmate.domain.notification.repository;

import com.back.catchmate.domain.notification.model.NotificationOutbox;
import com.back.catchmate.notifications.enums.ReferenceType;

import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository {
    NotificationOutbox save(NotificationOutbox outbox);
    List<NotificationOutbox> findAllPending(int maxRetryCount);
    List<NotificationOutbox> findAllPendingByRecipientId(Long recipientId);
    Optional<NotificationOutbox> findById(Long id);
    Optional<NotificationOutbox> findPendingByRecipientAndReference(Long recipientId, ReferenceType referenceType, Long referenceId);
}
