package com.back.catchmate.application.notification.service;

import com.back.catchmate.domain.notification.model.NotificationOutbox;
import com.back.catchmate.domain.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationOutboxUpdater {
    private final NotificationOutboxRepository outboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusSuccess(NotificationOutbox outbox) {
        outbox.success();
        outboxRepository.save(outbox);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusFailure(NotificationOutbox outbox, int maxRetryCount) {
        outbox.incrementRetryCount();
        if (outbox.getRetryCount() >= maxRetryCount) {
            outbox.fail();
        }
        outboxRepository.save(outbox);
    }
}
