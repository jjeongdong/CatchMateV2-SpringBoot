package com.back.catchmate.application.admin.event;

import com.back.catchmate.application.notification.port.NotificationDispatchPort;
import com.back.catchmate.application.notification.service.NotificationRetryService;
import com.back.catchmate.application.notification.service.NotificationService;
import com.back.catchmate.domain.notice.model.Notice;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.notifications.enums.NotificationChannel;
import com.back.catchmate.user.enums.AlarmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminNoticeCreateNotificationEventListener {
    private final NotificationService notificationService;
    private final NotificationRetryService notificationRetryService;
    private final NotificationDispatchPort notificationDispatchPort;

    @EventListener
    public void saveNoticeNotifications(AdminNoticeCreateNotificationEvent event) {
        Notice notice = event.notice();
        List<User> recipients = event.recipients();

        for (User recipient : recipients) {
            Notification notification = Notification.createNotification(
                    recipient,
                    null,
                    null,
                    event.title(),
                    AlarmType.EVENT,
                    notice.getId()
            );
            notificationService.createNotification(notification);

            if (recipient.getFcmToken() != null) {
                Map<String, String> data = Map.of(
                        "type", event.type(),
                        "noticeId", notice.getId().toString()
                );
                notificationRetryService.saveOutbox(
                        recipient.getId(),
                        recipient.getFcmToken(),
                        NotificationChannel.FCM,
                        event.title(),
                        event.body(),
                        data
                );
            }
        }
        log.info("공지사항 알림 아웃박스 저장 완료: noticeId={}, recipientCount={}", notice.getId(), recipients.size());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNoticeNotifications(AdminNoticeCreateNotificationEvent event) {
        Notice notice = event.notice();

        Map<String, String> stompPayload = Map.of(
                "type", event.type(),
                "noticeId", notice.getId().toString(),
                "title", event.title(),
                "body", event.body()
        );

        for (User recipient : event.recipients()) {
            notificationDispatchPort.dispatch(recipient.getId(), stompPayload);
            notificationRetryService.sendPendingOutboxImmediately(recipient.getId());
        }
    }
}
