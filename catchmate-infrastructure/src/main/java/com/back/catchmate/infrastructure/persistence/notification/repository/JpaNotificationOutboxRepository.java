package com.back.catchmate.infrastructure.persistence.notification.repository;

import com.back.catchmate.infrastructure.persistence.notification.entity.NotificationOutboxEntity;
import com.back.catchmate.notifications.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaNotificationOutboxRepository extends JpaRepository<NotificationOutboxEntity, Long> {
    List<NotificationOutboxEntity> findAllByStatusAndRetryCountLessThan(OutboxStatus status, int retryCount);
}
