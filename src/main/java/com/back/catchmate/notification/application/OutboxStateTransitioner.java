package com.back.catchmate.notification.application;

import com.back.catchmate.notification.domain.NotificationOutbox;
import com.back.catchmate.notification.domain.NotificationOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 상태 전이마다 짧은 REQUIRES_NEW 트랜잭션을 쓴다. 발송(FCM 호출)은 트랜잭션 밖에서 해서 커넥션을 잡지 않는다.
@Component
@RequiredArgsConstructor
public class OutboxStateTransitioner {
    private final MeterRegistry meterRegistry;
    private final NotificationOutboxRepository notificationOutboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<NotificationOutbox> claimPendingNotifications(int maxRetryCount, int batchSize) {
        List<NotificationOutbox> pending = notificationOutboxRepository.findPendingForUpdate(maxRetryCount, batchSize);
        if (pending.isEmpty()) {
            return pending;
        }
        pending.forEach(NotificationOutbox::startProcessing);
        notificationOutboxRepository.updateAll(pending);
        return pending;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<NotificationOutbox> claimPendingByRecipientId(Long recipientId) {
        List<NotificationOutbox> pending = notificationOutboxRepository.findPendingByRecipientIdForUpdate(recipientId);
        for (NotificationOutbox outbox : pending) {
            outbox.startProcessing();
            notificationOutboxRepository.save(outbox);
        }
        return pending;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<NotificationOutbox> claimPendingByRecipientIds(Collection<Long> recipientIds, int limit) {
        List<NotificationOutbox> pending =
                notificationOutboxRepository.findPendingByRecipientIdsForUpdate(recipientIds, limit);
        if (pending.isEmpty()) {
            return pending;
        }
        pending.forEach(NotificationOutbox::startProcessing);
        notificationOutboxRepository.updateAll(pending);
        return pending;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusSuccess(NotificationOutbox outbox) {
        outbox.success();
        notificationOutboxRepository.save(outbox);
        recordSuccessMetrics(outbox);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusPermanentFailure(NotificationOutbox outbox, String reason) {
        outbox.permanentFail(reason);
        notificationOutboxRepository.save(outbox);
        countPermanentFailure();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusFailure(NotificationOutbox outbox, int maxRetryCount, String errorMessage) {
        applyRetryableFailure(outbox, maxRetryCount, errorMessage);
        notificationOutboxRepository.save(outbox);
    }

    /**
     * 배치 발송 결과를 한 트랜잭션에서 일괄 확정한다.
     * <p>
     * 건별 {@code REQUIRES_NEW} 전이를 반복하면 배치 크기만큼 커넥션 획득·커밋이 생기므로
     * 성공/영구실패/재시도 세 갈래를 모아 한 번에 반영한다.
     *
     * @param errorMessages 아웃박스 id → 실패 사유(성공 건은 없음)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyDispatchResults(
            List<NotificationOutbox> successes,
            List<NotificationOutbox> permanentFailures,
            List<NotificationOutbox> retryableFailures,
            Map<Long, String> errorMessages,
            int maxRetryCount) {
        List<NotificationOutbox> updated =
                new ArrayList<>(successes.size() + permanentFailures.size() + retryableFailures.size());
        for (NotificationOutbox outbox : successes) {
            outbox.success();
            recordSuccessMetrics(outbox);
            updated.add(outbox);
        }
        for (NotificationOutbox outbox : permanentFailures) {
            outbox.permanentFail(errorMessages.get(outbox.getId()));
            countPermanentFailure();
            updated.add(outbox);
        }
        for (NotificationOutbox outbox : retryableFailures) {
            applyRetryableFailure(outbox, maxRetryCount, errorMessages.get(outbox.getId()));
            updated.add(outbox);
        }
        notificationOutboxRepository.updateAll(updated);
    }

    /**
     * 선점(PROCESSING) 후 발송 결과를 기록하지 못하고 정체된 행을 재시도 대상으로 되돌린다.
     * 발송이 실제로 나갔는지 알 수 없으므로 실패 1회로 계상해(retryCount++) 무한 회수를 막는다.
     * 회수는 곧 재발송이라 중복 푸시가 가능하며, 수신 측은 FCM data 의 dedupKey 로 이를 걸러낸다.
     *
     * @return 회수한 건수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recoverStuckProcessing(LocalDateTime threshold, int maxRetryCount, int batchSize) {
        List<NotificationOutbox> stuck =
                notificationOutboxRepository.findStuckProcessingForUpdate(threshold, batchSize);
        for (NotificationOutbox outbox : stuck) {
            applyRetryableFailure(outbox, maxRetryCount, "PROCESSING 상태 정체로 회수됨");
            notificationOutboxRepository.save(outbox);
            meterRegistry.counter("notification.outbox.recovered").increment();
        }
        return stuck.size();
    }

    private void applyRetryableFailure(NotificationOutbox outbox, int maxRetryCount, String errorMessage) {
        outbox.failRetryable(errorMessage, maxRetryCount);
        if (outbox.isFailed()) {
            meterRegistry
                    .counter("notification.outbox.failure", "type", "max_retry_exceeded")
                    .increment();
        }
    }

    private void countPermanentFailure() {
        meterRegistry
                .counter("notification.outbox.failure", "type", "permanent")
                .increment();
    }

    // 성공률 = success / (success + outbox.failure). retried 태그로 재시도 후 성공만 분리할 수 있다.
    private void recordSuccessMetrics(NotificationOutbox outbox) {
        String retried = outbox.getRetryCount() > 0 ? "true" : "false";
        meterRegistry.counter("notification.outbox.success", "retried", retried).increment();

        LocalDateTime createdAt = outbox.getCreatedAt();
        if (createdAt != null) {
            meterRegistry
                    .timer("notification.outbox.latency", "retried", retried)
                    .record(Duration.between(createdAt, LocalDateTime.now()));
        }
    }
}
