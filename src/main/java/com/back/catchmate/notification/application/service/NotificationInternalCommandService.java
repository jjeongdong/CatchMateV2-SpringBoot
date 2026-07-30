package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.NotificationInternalCommandUseCase;
import com.back.catchmate.notification.application.port.out.persistence.NotificationRepository;
import com.back.catchmate.notification.domain.model.Notification;
import com.back.catchmate.notification.domain.model.AlarmType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationInternalCommandService implements NotificationInternalCommandUseCase {
    private final NotificationRepository notificationRepository;

    @Override
    public void createNotification(Long userId, Long senderId, Long boardId, String title, AlarmType type, Long targetId) {
        Notification notification = Notification.createNotification(userId, senderId, boardId, title, type, targetId);
        notificationRepository.save(notification);
    }

    @Override
    public void createNotifications(List<Long> userIds, Long senderId, Long boardId, String title, AlarmType type, Long targetId) {
        if (userIds.isEmpty()) {
            return;
        }
        List<Notification> notifications = userIds.stream()
                .map(userId -> Notification.createNotification(userId, senderId, boardId, title, type, targetId))
                .toList();
        notificationRepository.saveAll(notifications);
    }
}
