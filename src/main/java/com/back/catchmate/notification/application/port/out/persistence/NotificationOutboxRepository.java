package com.back.catchmate.notification.application.port.out.persistence;

import com.back.catchmate.notification.domain.model.NotificationOutbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository {
    NotificationOutbox save(NotificationOutbox outbox);

    void saveAll(List<NotificationOutbox> outboxes);

    // 이미 적재된 행들의 상태 전이(선점·성공·실패)를 일괄 반영한다. 건별 save 왕복을 줄이기 위한 경로다.
    void updateAll(List<NotificationOutbox> outboxes);

    List<NotificationOutbox> findAllPending(int maxRetryCount, int batchSize);

    List<NotificationOutbox> findAllPendingByRecipientId(Long recipientId);

    // 선점(PROCESSING) 후 발송 결과를 기록하지 못하고 죽은 행. threshold 는 modifiedAt 기준 임계 시각.
    List<NotificationOutbox> findAllStuckProcessing(LocalDateTime threshold, int batchSize);

    Optional<NotificationOutbox> findById(Long id);
}
