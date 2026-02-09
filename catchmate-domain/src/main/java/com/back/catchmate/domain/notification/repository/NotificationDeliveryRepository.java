package com.back.catchmate.domain.notification.repository;

import com.back.catchmate.domain.notification.model.NotificationDelivery;
import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRepository {
    NotificationDelivery save(NotificationDelivery delivery);
    List<NotificationDelivery> findAllPending(int maxRetryCount);
    Optional<NotificationDelivery> findById(Long id);
}
