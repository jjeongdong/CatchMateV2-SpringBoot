package com.back.catchmate.notification.infrastructure;

import com.back.catchmate.notification.domain.NotificationOutbox;
import com.back.catchmate.notification.domain.OutboxStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query(
            "SELECT n FROM NotificationOutbox n WHERE n.status = :status AND n.modifiedAt < :threshold ORDER BY n.id ASC")
    List<NotificationOutbox> findAllStuckForRecovery(
            @Param("status") OutboxStatus status, @Param("threshold") LocalDateTime threshold, Pageable pageable);
}
