package com.back.catchmate.application.admin.event;

import com.back.catchmate.application.notification.service.NotificationRetryService;
import com.back.catchmate.application.notification.service.NotificationService;
import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.user.enums.AlarmType;
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
public class AdminInquiryAnswerNotificationEventListener {
    private final NotificationService notificationService;
    private final NotificationRetryService notificationRetryService;

    /**
     * 관리자 답변 알림을 아웃박스 패턴으로 저장합니다.
     */
    @EventListener
    public void saveInquiryNotification(AdminInquiryAnswerNotificationEvent event) {
        User recipient = event.recipient();
        Inquiry inquiry = event.inquiry();

        // 1. 알림 히스토리 저장
        Notification notification = Notification.createNotification(
                recipient,
                null,
                null,
                event.title(),
                AlarmType.INQUIRY_ANSWER,
                inquiry.getId()
        );
        notificationService.createNotification(notification);

        // 2. 푸시 발송을 위한 아웃박스 저장
        if (recipient.getFcmToken() != null && recipient.isEventAlarmEnabled()) {
            Map<String, String> data = Map.of(
                    "type", event.type(),
                    "inquiryId", inquiry.getId().toString()
            );

            notificationRetryService.saveOutbox(
                    recipient.getId(),
                    recipient.getFcmToken(),
                    event.title(),
                    event.body(),
                    data
            );
            log.info("관리자 답변 알림 아웃박스 저장 완료: recipientId: {}", recipient.getId());
        }
    }

    /**
     * 커밋 후 즉시 발송 시도
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInquiryNotification(AdminInquiryAnswerNotificationEvent event) {
        User recipient = event.recipient();
        if (recipient.isEventAlarmEnabled()) {
            notificationRetryService.sendPendingOutboxImmediately(recipient.getId());
        }
    }
}
