package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.domain.model.NotificationOutbox;
import com.back.catchmate.notification.application.port.out.persistence.NotificationOutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxStateTransitioner {
    private final MeterRegistry meterRegistry;
    private final NotificationOutboxRepository outboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<NotificationOutbox> claimPendingNotifications(int maxRetryCount, int batchSize) {
        List<NotificationOutbox> pendingList = outboxRepository.findAllPending(maxRetryCount, batchSize);
        if (pendingList.isEmpty()) {
            return pendingList;
        }
        pendingList.forEach(NotificationOutbox::startProcessing);
        outboxRepository.updateAll(pendingList);
        return pendingList;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<NotificationOutbox> claimPendingByRecipientId(Long recipientId) {
        List<NotificationOutbox> pendingList = outboxRepository.findAllPendingByRecipientId(recipientId);
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
        recordSuccessMetrics(outbox);
    }

    // 발송 성공률 / 재시도 후 성공률 / PENDING→SUCCESS 지연을 Prometheus 에서 집계할 수 있도록 계측.
    // 성공률 = success / (success + outbox.failure) 로 산출되며, retried 태그로 재시도 후 성공만 분리할 수 있다.
    private void recordSuccessMetrics(NotificationOutbox outbox) {
        String retried = outbox.getRetryCount() > 0 ? "true" : "false";
        meterRegistry.counter("notification.outbox.success", "retried", retried).increment();

        LocalDateTime createdAt = outbox.getCreatedAt();
        if (createdAt != null) {
            meterRegistry.timer("notification.outbox.latency", "retried", retried)
                    .record(Duration.between(createdAt, LocalDateTime.now()));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusPermanentFailure(NotificationOutbox outbox, String reason) {
        outbox.permanentFail(reason);
        outboxRepository.save(outbox);
        meterRegistry.counter("notification.outbox.failure", "type", "permanent").increment();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatusFailure(NotificationOutbox outbox, int maxRetryCount, String errorMessage) {
        applyFailure(outbox, maxRetryCount, errorMessage);
        outboxRepository.save(outbox);
    }

    /**
     * 배치 발송 결과를 한 트랜잭션에서 일괄 확정한다.
     * <p>
     * 건별 {@code REQUIRES_NEW} 전이를 반복하면 배치 크기만큼 커넥션 획득·커밋이 발생하므로,
     * 성공/영구실패/재시도 세 갈래를 모아 한 번에 반영한다.
     *
     * @param errorMessages 아웃박스 id → 실패 사유(성공 건은 없음)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyDispatchResults(List<NotificationOutbox> successes,
                                     List<NotificationOutbox> permanentFailures,
                                     List<NotificationOutbox> retryableFailures,
                                     Map<Long, String> errorMessages,
                                     int maxRetryCount) {
        List<NotificationOutbox> updated = new ArrayList<>(
                successes.size() + permanentFailures.size() + retryableFailures.size());

        for (NotificationOutbox outbox : successes) {
            outbox.success();
            recordSuccessMetrics(outbox);
            updated.add(outbox);
        }
        for (NotificationOutbox outbox : permanentFailures) {
            outbox.permanentFail(errorMessages.get(outbox.getId()));
            meterRegistry.counter("notification.outbox.failure", "type", "permanent").increment();
            updated.add(outbox);
        }
        for (NotificationOutbox outbox : retryableFailures) {
            applyFailure(outbox, maxRetryCount, errorMessages.get(outbox.getId()));
            updated.add(outbox);
        }

        outboxRepository.updateAll(updated);
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
        List<NotificationOutbox> stuckList = outboxRepository.findAllStuckProcessing(threshold, batchSize);
        for (NotificationOutbox outbox : stuckList) {
            applyFailure(outbox, maxRetryCount, "PROCESSING 상태 정체로 회수됨");
            outboxRepository.save(outbox);
            meterRegistry.counter("notification.outbox.recovered").increment();
        }
        return stuckList.size();
    }

    private void applyFailure(NotificationOutbox outbox, int maxRetryCount, String errorMessage) {
        outbox.incrementRetryCount();
        outbox.recordError(errorMessage);
        if (outbox.getRetryCount() >= maxRetryCount) {
            outbox.fail();
            meterRegistry.counter("notification.outbox.failure", "type", "max_retry_exceeded").increment();
        } else {
            outbox.pending();
        }
    }
}
