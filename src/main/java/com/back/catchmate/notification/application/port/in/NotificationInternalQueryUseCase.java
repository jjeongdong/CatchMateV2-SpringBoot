package com.back.catchmate.notification.application.port.in;

import com.back.catchmate.notification.domain.model.Notification;

public interface NotificationInternalQueryUseCase {
    Notification getNotification(Long notificationId);
}
