package com.back.catchmate.notification.infrastructure;

import com.back.catchmate.notification.domain.NotificationOutbox;
import com.back.catchmate.notification.domain.OutboxStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

// lock.timeout = -2 는 Hibernate 의 SKIP LOCKED 다. 여러 인스턴스가 같은 행을 동시에 선점하지 않게 한다.
public interface NotificationOutboxJpaRepository extends JpaRepository<NotificationOutbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT n FROM NotificationOutbox n WHERE n.status = :status AND n.retryCount < :retryCount")
    List<NotificationOutbox> findAllForProcessing(
            @Param("status") OutboxStatus status, @Param("retryCount") int retryCount, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query(
            "SELECT n FROM NotificationOutbox n WHERE n.recipientId = :recipientId AND n.status = :status ORDER BY n.id ASC")
    List<NotificationOutbox> findAllByRecipientIdAndStatusForProcessing(
            @Param("recipientId") Long recipientId, @Param("status") OutboxStatus status);

    // 네이티브 쿼리: 행이 적거나 통계가 치우치면 옵티마이저가 (status, retry_count) 인덱스로 PENDING 전체를 훑고,
    // FOR UPDATE 가 훑은 행을 모두 잠가 남의 수신자 행까지 선점을 막는다. 수신자 인덱스로 고정해 대상 행만 잠근다.
    // 정렬은 인덱스 순서(recipient_id, status, id)와 맞춘다. ORDER BY id 만 쓰면 filesort 라 조건에 맞는 행을
    // 전부 잠근 뒤 LIMIT 으로 자른다. 대가로 밀린 알림이 한도를 넘으면 수신자 ID 순으로 먼저 나간다.
    @Query(
            value =
                    """
                    SELECT * FROM notification_outbox FORCE INDEX (idx_outbox_recipient_status)
                    WHERE recipient_id IN (:recipientIds) AND status = 'PENDING'
                    ORDER BY recipient_id ASC, id ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<NotificationOutbox> findAllPendingByRecipientIdsForProcessing(
            @Param("recipientIds") Collection<Long> recipientIds, @Param("limit") int limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query(
            "SELECT n FROM NotificationOutbox n WHERE n.status = :status AND n.modifiedAt < :threshold ORDER BY n.id ASC")
    List<NotificationOutbox> findAllStuckForRecovery(
            @Param("status") OutboxStatus status, @Param("threshold") LocalDateTime threshold, Pageable pageable);
}
