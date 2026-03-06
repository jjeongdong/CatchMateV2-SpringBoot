package com.back.catchmate.application.notification.service;

import com.back.catchmate.domain.notification.model.NotificationOutbox;
import com.back.catchmate.domain.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationOutboxUpdater {
    private final NotificationOutboxRepository outboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<NotificationOutbox> claimPendingNotifications(int maxRetryCount) {
        List<NotificationOutbox> pendingList = outboxRepository.findAllPending(maxRetryCount);
        for (NotificationOutbox outbox : pendingList) {
            outbox.startProcessing();
            outboxRepository.save(outbox);
        }
        return pendingList;
    }

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
        } else {
            // 다시 PENDING으로 되돌려 다음 주기에 시도할 수 있게 함
            outbox.pending();
        }
        outboxRepository.save(outbox);
    }
}
