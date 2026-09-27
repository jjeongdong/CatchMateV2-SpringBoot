package com.back.catchmate.notification.application;

import com.back.catchmate.notification.application.dto.result.NotificationReadAllResult;
import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationCommandService {
    private final NotificationRepository notificationRepository;

    @Transactional
    public void markNotificationAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.getById(notificationId);
        notification.verifyOwner(userId);
        notification.markAsRead();
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.getById(notificationId);
        notification.verifyOwner(userId);
        notificationRepository.delete(notification);
    }

    @Transactional
    public NotificationReadAllResult readAllNotifications(Long userId) {
        return new NotificationReadAllResult(notificationRepository.markAllReadByUserId(userId));
    }
}
