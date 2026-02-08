package com.back.catchmate.application.enroll.event;

import com.back.catchmate.application.notification.service.NotificationService;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.notification.port.NotificationSender;
import com.back.catchmate.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import user.enums.AlarmType;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EnrollNotificationEventListener {
    private final NotificationSender notificationSender;
    private final NotificationService notificationService;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEnrollNotification(EnrollNotificationEvent event) {
        // 1) 알림 저장은 사용자 온라인/FCM 여부와 무관하게 항상 수행
        Notification notification = Notification.createNotification(
                event.recipient(),
                event.sender(),
                event.board(),
                event.title(),
                AlarmType.ENROLL,
                event.referenceId()
        );
        notificationService.createNotification(notification);

        // 2) 푸시는 토큰/설정 체크 후 오프라인 사용자에게만 전송
        User recipient = event.recipient();
        if (recipient.getFcmToken() == null || recipient.getEnrollAlarm() != 'Y') {
            return;
        }

        Map<String, String> data = Map.of(
                "type", event.type(),
                "boardId", event.board().getId().toString()
        );

        notificationSender.sendNotificationIfOffline(
                recipient.getId(),
                recipient.getFcmToken(),
                event.title(),
                event.body(),
                data
        );
    }
}
