package com.back.catchmate.infrastructure.persistence.notification.repository;

import com.back.catchmate.infrastructure.persistence.notification.entity.NotificationDeliveryEntity;
import com.back.catchmate.notifications.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaNotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, Long> {
    List<NotificationDeliveryEntity> findAllByStatusAndRetryCountLessThan(DeliveryStatus status, int retryCount);
}
