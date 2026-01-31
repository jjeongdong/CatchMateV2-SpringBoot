package com.back.catchmate.domain.notification.repository;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.notification.model.Notification;

import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    DomainPage<Notification> findAllByUserId(Long userId, DomainPageable pageable);

    void delete(Notification notification);
}
