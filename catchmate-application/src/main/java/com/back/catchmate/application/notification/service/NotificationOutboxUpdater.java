package com.back.catchmate.application.notification.service;

import com.back.catchmate.domain.notification.model.NotificationOutbox;
import com.back.catchmate.domain.notification.repository.NotificationOutboxRepository;
import com.back.catchmate.notifications.enums.ReferenceType;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxUpdater {
    private final NotificationOutboxRepository outboxRepository;
    private final MeterRegistry meterRegistry;

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
            meterRegistry.counter("notification.outbox.failure", "type", "max_retry_exceeded").increment();
        } else {
            // 다시 PENDING으로 되돌려 다음 주기에 시도할 수 있게 함
            outbox.pending();
        }
        outboxRepository.save(outbox);
    }

    /**
     * WebSocket으로 알림이 이미 전달된 outbox row를 SKIPPED 처리한다.
     * Phase 2 (@TransactionalEventListener AFTER_COMMIT, @Async) 에서 호출되므로
     * REQUIRES_NEW로 별도 트랜잭션에서 안전하게 close한다.
     * <p>
     * 스케줄러가 이미 row를 PROCESSING으로 락 잡았다면 PENDING으로 못 찾아 no-op으로 무시.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSkippedByReference(Long recipientId, ReferenceType referenceType, Long referenceId) {
        outboxRepository.findPendingByRecipientAndReference(recipientId, referenceType, referenceId)
                .ifPresent(outbox -> {
                    outbox.skip();
                    outboxRepository.save(outbox);
                    meterRegistry.counter("notification.outbox.skipped",
                            "reference_type", referenceType.name()).increment();
                    log.debug("Outbox SKIPPED (WebSocket으로 전달됨): recipientId={}, refType={}, refId={}",
                            recipientId, referenceType, referenceId);
                });
    }
}
