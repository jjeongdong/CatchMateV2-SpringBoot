package com.back.catchmate.application.notification.service;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.notification.repository.NotificationRepository;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public void createNotification(Notification notification) {
        notificationRepository.save(notification);
    }

    // 조회와 읽음 처리를 함께 수행하는 비즈니스 메서드
    public Notification getNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }
        
        return notification;
    }

    public DomainPage<Notification> getNotificationList(Long userId, DomainPageable pageable) {
        return notificationRepository.findAllByUserId(userId, pageable);
    }

    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 소유권 검증 로직이 필요하다면 여기에 추가 (현재는 AOP에서 처리 중)
        notificationRepository.delete(notification);
    }

    public boolean hasUnreadNotifications(Long userId) {
        return notificationRepository.hasUnreadNotifications(userId);
    }
}
