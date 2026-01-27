package com.back.catchmate.domain.notification.repository;

import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;
import com.back.catchmate.domain.notification.model.Notification;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    DomainPage<Notification> findAllByUserId(Long userId, DomainPageable pageable);
    Optional<Notification> findById(Long notificationId);
}
