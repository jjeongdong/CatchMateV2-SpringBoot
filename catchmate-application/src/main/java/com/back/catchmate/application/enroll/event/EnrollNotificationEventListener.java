package com.back.catchmate.application.enroll.event;

import com.back.catchmate.application.notification.event.NotificationEvent;
import com.back.catchmate.application.notification.service.NotificationRetryService;
import com.back.catchmate.application.notification.service.NotificationService;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.port.UserOnlineStatusPort;
import com.back.catchmate.notifications.enums.NotificationChannel;
import com.back.catchmate.user.enums.AlarmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 메인 트랜잭션 내에서 실행되어 알림 엔티티와 아웃박스 데이터를 저장함
     */
    @EventListener
    public void saveNotification(EnrollNotificationEvent event) {
        // 1. 알림 히스토리 저장
        Notification notification = Notification.createNotification(
                event.recipient(),
                event.sender(),
                event.board(),
                event.title(),
                AlarmType.ENROLL,
                event.referenceId()
        );
        notificationService.createNotification(notification);

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
        boolean isOnline = userOnlineStatusPort.isUserOnline(recipient.getId());

        if (isOnline) {
            sendWebSocketNotification(recipient.getId(), payload);
        } else {
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

    private void sendWebSocketNotification(Long userId, Map<String, String> payload) {
        try {
            applicationEventPublisher.publishEvent(NotificationEvent.of(userId, payload));
        } catch (Exception e) {
            log.warn("WebSocket notification failed. userId: {}", userId, e);
        }
    }
}
