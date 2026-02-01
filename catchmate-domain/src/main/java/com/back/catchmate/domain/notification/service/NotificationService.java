package com.back.catchmate.domain.notification.service;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.notification.repository.NotificationRepository;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public void createNotification(Notification notification) {
        notificationRepository.save(notification);
    }

    public Notification getNotification(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    public DomainPage<Notification> getNotificationList(Long userId, DomainPageable pageable) {
        return notificationRepository.findAllByUserId(userId, pageable);
    }

    public void updateNotification(Notification notification) {
        notificationRepository.save(notification);
    }

    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notificationRepository.delete(notification);
    }

    public boolean hasUnreadNotifications(Long userId) {
        return notificationRepository.hasUnreadNotifications(userId);
    }
}
