package com.back.catchmate.application.enroll.event;

import com.back.catchmate.domain.notification.port.NotificationSender;
import com.back.catchmate.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EnrollNotificationEventListener {
    private final NotificationSender notificationSender;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEnrollNotification(EnrollNotificationEvent event) {
        User recipient = event.recipient();

        if (recipient.getFcmToken() == null || recipient.getEnrollAlarm() != 'Y') {
            return;
        }

        Map<String, String> data = Map.of(
                "type", event.type(),
                "boardId", event.board().getId().toString()
        );

        // 오프라인 사용자에게만 FCM 알림 전송
        notificationSender.sendNotificationIfOffline(
                recipient.getId(),
                recipient.getFcmToken(),
                event.title(),
                event.body(),
                data
        );
    }
}
