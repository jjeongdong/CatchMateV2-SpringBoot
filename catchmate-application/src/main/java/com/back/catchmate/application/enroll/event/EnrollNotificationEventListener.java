package com.back.catchmate.application.enroll.event;

import com.back.catchmate.application.chat.port.MessagePublisher;
import com.back.catchmate.application.notification.event.NotificationEvent;
import com.back.catchmate.application.notification.service.NotificationRetryService;
import com.back.catchmate.application.notification.service.NotificationService;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.notification.port.NotificationSender;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.port.UserOnlineStatusPort;
import com.back.catchmate.user.enums.AlarmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollNotificationEventListener {
    private final NotificationSender notificationSender;
    private final NotificationService notificationService;
    private final NotificationRetryService notificationRetryService;
    private final UserOnlineStatusPort userOnlineStatusPort;
    private final MessagePublisher messagePublisher;

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

        Map<String, String> data = Map.of(
                "type", event.type(),
                "boardId", event.board().getId().toString(),
                "title", event.title(),
                "body", event.body()
        );

        // 2. [WebSocket] 사용자 온라인 여부 확인 -> 토큰 없어도 전송 가능
        try {
            boolean isOnline = userOnlineStatusPort.isUserOnline(recipient.getId());
            if (isOnline) {
                messagePublisher.publishNotification(NotificationEvent.of(recipient.getId(), data));
            }
        } catch (Exception e) {
            log.warn("WebSocket notification failed", e);
        }

        // 3. 무조건 FCM 전송 (신청 중요 알림은 시스템 트레이에 남기기 위해)
        if (recipient.getFcmToken() != null) {
            try {
                notificationSender.sendNotification(
                        recipient.getFcmToken(),
                        event.title(),
                        event.body(),
                        data
                );
            } catch (Exception e) {
                log.warn("푸시 전송 실패 -> DLQ 저장 시도. recipientId: {}", event.recipient().getId());

                // 전송 실패 시 DLQ 저장
                try {
                    Map<String, String> retryData = Map.of(
                            "type", event.type(),
                            "boardId", event.board().getId().toString(),
                            "title", event.title(),
                            "body", event.body()
                    );

                    notificationRetryService.saveFailedNotification(
                            event.recipient().getId(),
                            event.recipient().getFcmToken(),
                            event.title(),
                            event.body(),
                            retryData
                    );

                } catch (Exception dlqError) {
                    log.error("DLQ 저장조차 실패했습니다.", dlqError);
                }
            }
        }
    }
}
