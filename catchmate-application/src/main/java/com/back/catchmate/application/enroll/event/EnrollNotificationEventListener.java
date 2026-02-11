package com.back.catchmate.application.enroll.event;

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
    private final SimpMessagingTemplate messagingTemplate;
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

        // 토큰이나 알림 설정이 꺼져있으면 아예 시도하지 않음
        if (recipient.getFcmToken() == null || recipient.getEnrollAlarm() != 'Y') {
            return;
        }

        Map<String, String> data = Map.of(
                "type", event.type(),
                "boardId", event.board().getId().toString(),
                "title", event.title(),
                "body", event.body()
        );

        // 2. 사용자가 앱에 접속 중(Online)인지 확인
        boolean isOnline = userOnlineStatusPort.isUserOnline(recipient.getId());

        if (isOnline) {
            // [A] 앱 접속 중 -> WebSocket으로 실시간 전송
            // 클라이언트는 "/user/queue/notifications"를 구독하고 있어야 함
            try {
                messagingTemplate.convertAndSendToUser(
                        recipient.getId().toString(),
                        "/queue/notifications",
                        data
                );
                log.info("WebSocket notification sent to User {}", recipient.getId());
            } catch (Exception e) {
                log.error("WebSocket sending failed", e);
            }
        }

        // 3. FCM 전송 판단
        // 전략 1: 오프라인일 때만 FCM 전송 (채팅 등 일반 알림)
        // 전략 2: 무조건 FCM 전송 (신청/합격 등 중요 알림은 시스템 트레이에 남기기 위해)
        try {
            if (recipient.getFcmToken() != null && recipient.getEnrollAlarm() == 'Y') {
                notificationSender.sendNotification(
                        recipient.getFcmToken(),
                        event.title(),
                        event.body(),
                        data
                );
            }

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
