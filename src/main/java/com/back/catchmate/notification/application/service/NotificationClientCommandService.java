package com.back.catchmate.notification.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.notification.application.port.in.NotificationClientCommandUseCase;
import com.back.catchmate.notification.application.port.out.persistence.NotificationRepository;
import com.back.catchmate.notification.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationClientCommandService implements NotificationClientCommandUseCase {
    private final NotificationRepository notificationRepository;
    private final NotificationReader notificationReader;

    @Override
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationReader.getNotification(notificationId);
        verifyOwner(notification, userId);
        notificationRepository.delete(notification);
    }

    @Override
    public int readAllNotifications(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    @Override
    public void markNotificationAsRead(Long userId, Long notificationId) {
        Notification notification = notificationReader.getNotification(notificationId);
        verifyOwner(notification, userId);
        if (notification.isRead()) return;
        notification.markAsRead();
        notificationRepository.save(notification);
    }

    private void verifyOwner(Notification notification, Long userId) {
        if (!notification.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }
    }
}
