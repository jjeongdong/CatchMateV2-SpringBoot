package com.back.catchmate.infrastructure.persistence.notification.repository;

import com.back.catchmate.infrastructure.persistence.notification.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findAllByUserId(Long userId, Pageable pageable);
}
