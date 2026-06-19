package com.back.catchmate.notification.application.port.in;

public interface NotificationClientCommandUseCase {
    void deleteNotification(Long userId, Long notificationId);

    int readAllNotifications(Long userId);

    void markNotificationAsRead(Long userId, Long notificationId);
}
