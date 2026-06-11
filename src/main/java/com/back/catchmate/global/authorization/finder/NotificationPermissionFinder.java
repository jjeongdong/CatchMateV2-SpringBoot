package com.back.catchmate.global.authorization.finder;

import com.back.catchmate.notification.application.service.NotificationService;
import com.back.catchmate.global.authorization.common.DomainFinder;
import com.back.catchmate.notification.domain.model.Notification;
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
