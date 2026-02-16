package com.back.catchmate.application.enroll.event;

import com.back.catchmate.application.chat.port.MessagePublisherPort;
import com.back.catchmate.application.notification.event.NotificationEvent;
import com.back.catchmate.application.notification.service.NotificationRetryService;
import com.back.catchmate.application.notification.service.NotificationService;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.notification.port.NotificationSenderPort;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.port.UserOnlineStatusPort;
import com.back.catchmate.user.enums.AlarmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollNotificationEventListener {
    private final NotificationSenderPort notificationSenderPort;
    private final NotificationService notificationService;
    private final NotificationRetryService notificationRetryService;
    private final MessagePublisherPort messagePublisherPort;
    private final UserOnlineStatusPort userOnlineStatusPort;

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
        User recipient = event.recipient();

        // 1. 알림 설정(On/Off) 확인
        if (recipient.getEnrollAlarm() != 'Y') {
            return;
        }

        // 2. 공통 데이터 생성
        Map<String, String> payload = createNotificationData(event);

        // 3. WebSocket 알림 전송 (실시간)
        sendWebSocketNotification(recipient.getId(), payload);

        // 4. FCM 푸시 알림 전송 (실시간, 실패 시 DLQ 저장)
        sendFcmNotificationWithFallback(recipient, event, payload);
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
            if (userOnlineStatusPort.isUserOnline(userId)) {
                messagePublisherPort.publishNotification(NotificationEvent.of(userId, payload));
            }
        } catch (Exception e) {
            log.warn("WebSocket notification failed. userId: {}", userId, e);
        }
    }

    private void sendFcmNotificationWithFallback(User recipient, EnrollNotificationEvent event, Map<String, String> payload) {
        if (recipient.getFcmToken() == null) {
            return;
        }

        try {
            notificationSenderPort.sendNotification(
                    recipient.getFcmToken(),
                    event.title(),
                    event.body(),
                    payload
            );
        } catch (Exception e) {
            log.warn("푸시 전송 실패 -> DLQ 저장 시도. recipientId: {}", recipient.getId());
            saveToDeadLetterQueue(recipient, event, payload);
        }
    }

    private void saveToDeadLetterQueue(User recipient, EnrollNotificationEvent event, Map<String, String> payload) {
        try {
            notificationRetryService.saveFailedNotification(
                    recipient.getId(),
                    recipient.getFcmToken(),
                    event.title(),
                    event.body(),
                    payload
            );
        } catch (Exception dlqError) {
            log.error("DLQ 저장조차 실패했습니다. recipientId: {}", recipient.getId(), dlqError);
        }
    }
}
