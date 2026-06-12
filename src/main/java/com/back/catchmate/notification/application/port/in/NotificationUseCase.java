package com.back.catchmate.notification.application.port.in;

import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.notification.application.dto.response.NotificationResponse;
import com.back.catchmate.notification.application.dto.response.UnreadNotificationResponse;

public interface NotificationUseCase {
    NotificationResponse getNotification(Long userId, Long notificationId);
    PagedResponse<NotificationResponse> getNotificationList(Long userId, int page, int size);
    void deleteNotification(Long userId, Long notificationId);
    UnreadNotificationResponse hasUnreadNotifications(Long userId);
    int readAllNotifications(Long userId);
    void processPendingNotifications();
}
