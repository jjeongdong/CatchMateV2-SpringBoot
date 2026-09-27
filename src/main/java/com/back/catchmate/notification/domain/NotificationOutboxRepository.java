package com.back.catchmate.notification.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationOutboxRepository {

    NotificationOutbox save(NotificationOutbox outbox);

    // 수신자 수만큼의 아웃박스를 한 번의 멀티로우 INSERT 로 적재한다.
    void saveAllInBatch(List<NotificationOutbox> outboxes);

    // 이미 적재된 행들의 상태 전이를 한 번의 배치 UPDATE 로 반영한다.
    void updateAll(List<NotificationOutbox> outboxes);

    // 아래 셋은 비관적 락 + SKIP LOCKED 로 조회한다. 다른 인스턴스가 잡은 행은 건너뛴다.
    List<NotificationOutbox> findPendingForUpdate(int maxRetryCount, int limit);

    List<NotificationOutbox> findPendingByRecipientIdForUpdate(Long recipientId);

    List<NotificationOutbox> findStuckProcessingForUpdate(LocalDateTime threshold, int limit);
}
