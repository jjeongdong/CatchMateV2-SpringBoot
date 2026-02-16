package com.back.catchmate.orchestration.notification;

import com.back.catchmate.application.notification.service.NotificationRetryService;
import com.back.catchmate.application.notification.service.NotificationService;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.orchestration.common.PagedResponse;
import com.back.catchmate.orchestration.notification.dto.response.NotificationResponse;
import com.back.catchmate.orchestration.notification.dto.response.UnreadNotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationOrchestrator {
    private final NotificationService notificationService;
    private final NotificationRetryService notificationRetryService;

    @Transactional
    public NotificationResponse getNotification(Long userId, Long notificationId) {
        // 1. 서비스 호출 (조회 및 읽음 처리 로직 위임)
        Notification notification = notificationService.getNotification(notificationId);

        // 2. DTO 변환 및 반환
        return NotificationResponse.from(notification);
    }

    public PagedResponse<NotificationResponse> getNotificationList(Long userId, int page, int size) {
        DomainPageable domainPageable = DomainPageable.of(page, size);
        
        // 1. 서비스 호출
        DomainPage<Notification> notificationPage = notificationService.getNotificationList(userId, domainPageable);

        // 2. DTO 변환
        List<NotificationResponse> responses = notificationPage.getContent().stream()
                .map(NotificationResponse::from)
                .toList();

        return new PagedResponse<>(notificationPage, responses);
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        notificationService.deleteNotification(userId, notificationId);
    }

    public UnreadNotificationResponse hasUnreadNotifications(Long userId) {
        boolean hasUnread = notificationService.hasUnreadNotifications(userId);
        return UnreadNotificationResponse.of(hasUnread);
    }

    public void retryFailedNotifications() {
        notificationRetryService.retryFailedNotifications();
    }
}
