package com.back.catchmate.notification.application.port.out;

import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.notification.domain.model.Notification;

import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    DomainPage<Notification> findAllByUserId(Long userId, DomainPageable pageable);

    void delete(Notification notification);

    boolean hasUnreadNotifications(Long userId);

    int markAllRead(Long userId);
}
