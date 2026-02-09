package com.back.catchmate.application.enroll.event;

import com.back.catchmate.application.notification.service.NotificationService;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.notification.port.NotificationSender;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.user.enums.AlarmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollNotificationEventListener {
    private final NotificationSender notificationSender;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void saveNotification(EnrollNotificationEvent event) {
        Notification notification = Notification.createNotification(
                event.recipient(),
                event.sender(),
                event.board(),
                event.title(),
                AlarmType.ENROLL,
                event.referenceId()
        );
        notificationService.createNotification(notification);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEnrollNotification(EnrollNotificationEvent event) {
        try {
            User recipient = event.recipient();

            // 토큰이나 알림 설정이 꺼져있으면 아예 시도하지 않음
            if (recipient.getFcmToken() == null || recipient.getEnrollAlarm() != 'Y') {
                return;
            }

            Map<String, String> data = Map.of(
                    "type", event.type(),
                    "boardId", event.board().getId().toString()
            );

            // Sender 내부에서 실패 시 알아서 3회 재시도함
            notificationSender.sendNotificationIfOffline(
                    recipient.getId(),
                    recipient.getFcmToken(),
                    event.title(),
                    event.body(),
                    data
            );

        } catch (Exception e) {
            // DB는 이미 저장되었으므로, 여기서는 로그만 남기고 조용히 종료
            log.warn("알림 DB 저장은 성공했으나, 푸시 전송에 최종 실패했습니다. - recipientId: {}", event.recipient().getId());
        }
    }
}
