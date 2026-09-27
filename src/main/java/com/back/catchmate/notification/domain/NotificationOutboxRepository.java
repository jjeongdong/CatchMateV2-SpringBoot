package com.back.catchmate.notification.domain;

import java.time.LocalDateTime;
import java.util.Collection;
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

    // 대기 알림이 밀려 있어도 한 번에 발송·확정하는 행 수를 limit 로 묶는다. 나머지는 스케줄러가 이어 보낸다.
    List<NotificationOutbox> findPendingByRecipientIdsForUpdate(Collection<Long> recipientIds, int limit);

    List<NotificationOutbox> findStuckProcessingForUpdate(LocalDateTime threshold, int limit);

    // 처리가 끝난 행 중 threshold 이전에 끝난 것을 limit 건까지 지우고 지운 건수를 돌려준다. 호출마다 따로 커밋된다.
    int deleteFinishedBefore(Collection<OutboxStatus> statuses, LocalDateTime threshold, int limit);
}
