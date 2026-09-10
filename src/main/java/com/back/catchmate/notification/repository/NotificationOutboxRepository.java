package com.back.catchmate.notification.repository;

import com.back.catchmate.notification.entity.NotificationOutbox;
import com.back.catchmate.notification.entity.enums.OutboxStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long>, NotificationOutboxRepositoryCustom {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT n FROM NotificationOutbox n WHERE n.status = :status AND n.retryCount < :retryCount")
    List<NotificationOutbox> findAllForProcessing(OutboxStatus status, int retryCount, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT n FROM NotificationOutbox n WHERE n.recipientId = :recipientId AND n.status = :status ORDER BY n.id ASC")
    List<NotificationOutbox> findAllByRecipientIdAndStatusForProcessing(Long recipientId, OutboxStatus status);

    // 선점 후 결과 기록에 실패해 정체된 행. 다른 인스턴스가 이미 회수 중인 행은 SKIP LOCKED 로 건너뛴다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT n FROM NotificationOutbox n WHERE n.status = :status AND n.modifiedAt < :threshold ORDER BY n.id ASC")
    List<NotificationOutbox> findAllStuckForRecovery(OutboxStatus status, LocalDateTime threshold, Pageable pageable);
}
