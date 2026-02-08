package com.back.catchmate.application.notification.service;

import com.back.catchmate.domain.common.permission.DomainFinder;
import com.back.catchmate.domain.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPermissionFinder implements DomainFinder<Notification> {
    private final NotificationService notificationService;

    @Override
    public Notification searchById(Long notificationId) {
        return notificationService.getNotification(notificationId);
    }
}
