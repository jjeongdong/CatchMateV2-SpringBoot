package com.back.catchmate.application.admin.event;

import com.back.catchmate.application.notification.service.NotificationService;
import com.back.catchmate.domain.inquiry.model.Inquiry;
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
import com.back.catchmate.user.enums.AlarmType;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdminInquiryAnswerNotificationEventListener {
    private final NotificationSender notificationSender;
    private final NotificationService notificationService;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AdminInquiryAnswerNotificationEvent event) {
        User recipient = event.recipient();
        Inquiry inquiry = event.inquiry();

        Notification notification = Notification.createNotification(
                recipient,
                null,
                null,
                event.title(),
                AlarmType.INQUIRY_ANSWER,
                inquiry.getId()
        );
        notificationService.createNotification(notification);

        if (recipient.getFcmToken() == null || recipient.getEventAlarm() != 'Y') {
            return;
        }

        Map<String, String> data = Map.of(
                "type", event.type(),
                "inquiryId", inquiry.getId().toString()
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

