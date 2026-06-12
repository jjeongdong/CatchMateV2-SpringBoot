package com.back.catchmate.enroll.application.event;

import com.back.catchmate.notification.application.port.out.NotificationDispatchPort;
import com.back.catchmate.notification.application.service.NotificationRetryService;
import com.back.catchmate.notification.application.service.NotificationService;
import com.back.catchmate.notification.domain.model.Notification;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.user.application.port.out.UserOnlineStatusPort;
import com.back.catchmate.notification.domain.enums.NotificationChannel;
import com.back.catchmate.user.domain.enums.AlarmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollNotificationEventListener {
    private final NotificationService notificationService;
    private final NotificationRetryService notificationRetryService;
    private final UserOnlineStatusPort userOnlineStatusPort;
    private final NotificationDispatchPort notificationDispatchPort;

    /**
     * 메인 트랜잭션 내에서 실행되어 알림 엔티티와 아웃박스 데이터를 저장함
     */
    @EventListener
    public void saveNotification(EnrollNotificationEvent event) {
        // 1. 알림 히스토리 저장
        Notification notification = Notification.createNotification(
                event.recipient().getId(),
                event.sender() != null ? event.sender().getId() : null,
                event.board() != null ? event.board().getId() : null,
                event.title(),
                AlarmType.ENROLL,
                event.referenceId()
        );
        notificationService.createNotification(notification);

        if (!event.pushEnabled()) return;

        // 2. 푸시 발송을 위한 아웃박스 저장
        User recipient = event.recipient();
        if (recipient.isEnrollAlarmEnabled() && recipient.getFcmToken() != null) {
            Map<String, String> payload = createNotificationData(event);
            notificationRetryService.saveOutbox(
                    recipient.getId(),
                    recipient.getFcmToken(),
                    NotificationChannel.FCM,
                    event.title(),
                    event.body(),
                    payload
            );
        }
    }

    /**
     * 커밋 후 즉시 발송 시도 (Best effort)
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEnrollNotification(EnrollNotificationEvent event) {
        User recipient = event.recipient();
        if (!recipient.isEnrollAlarmEnabled()) return;

        Map<String, String> payload = createNotificationData(event);

        // WebSocket 은 항상 시도 — 구독자가 있으면 전달되고, 없으면 silently drop 됨
        notificationDispatchPort.dispatch(recipient.getId(), payload);

        // 오프라인이고 push 가 활성화된 케이스는 FCM 으로 보강 발송
        if (event.pushEnabled() && !userOnlineStatusPort.isUserOnline(recipient.getId())) {
            notificationRetryService.sendPendingOutboxImmediately(recipient.getId());
        }
    }

    private Map<String, String> createNotificationData(EnrollNotificationEvent event) {
        return Map.of(
                "type", event.type(),
                "boardId", event.board().getId().toString(),
                "title", event.title(),
                "body", event.body()
        );
    }
}
