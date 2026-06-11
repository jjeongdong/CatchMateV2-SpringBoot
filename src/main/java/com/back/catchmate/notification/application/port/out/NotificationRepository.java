package com.back.catchmate.notification.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.notification.domain.model.Notification;

import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    Page<Notification> findAllByUserId(Long userId, Pageable pageable);

    void delete(Notification notification);

    boolean hasUnreadNotifications(Long userId);

    int markAllRead(Long userId);
}
