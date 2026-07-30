package com.back.catchmate.notification.application.port.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.notification.domain.model.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);

    void saveAll(List<Notification> notifications);

    Optional<Notification> findById(Long id);

    Page<Notification> findAllByUserId(Long userId, Pageable pageable);

    void delete(Notification notification);

    boolean hasUnreadNotifications(Long userId);

    int markAllRead(Long userId);
}
