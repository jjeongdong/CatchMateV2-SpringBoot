package com.back.catchmate.infrastructure.persistence.notification.repository;

import com.back.catchmate.infrastructure.persistence.notification.entity.NotificationOutboxEntity;
import com.back.catchmate.notifications.enums.OutboxStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;

public interface JpaNotificationOutboxRepository extends JpaRepository<NotificationOutboxEntity, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")}) // SKIP LOCKED (PostgreSQL/MySQL 8.0+)
    @Query("SELECT n FROM NotificationOutboxEntity n WHERE n.status = :status AND n.retryCount < :retryCount")
    List<NotificationOutboxEntity> findAllForProcessing(OutboxStatus status, int retryCount);

    List<NotificationOutboxEntity> findAllByRecipientIdAndStatus(Long recipientId, OutboxStatus status);
}
