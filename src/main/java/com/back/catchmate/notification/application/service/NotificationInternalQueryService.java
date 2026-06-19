package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.NotificationInternalQueryUseCase;
import com.back.catchmate.notification.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationInternalQueryService implements NotificationInternalQueryUseCase {
    private final NotificationReader notificationReader;

    @Override
    public Notification getNotification(Long notificationId) {
        return notificationReader.getNotification(notificationId);
    }
}
