package com.back.catchmate.notification.application;

import com.back.catchmate.notification.domain.AlarmType;
import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.NotificationPayload;
import com.back.catchmate.notification.domain.NotificationRepository;
import com.back.catchmate.notification.domain.NotificationTemplate;
import com.back.catchmate.notification.domain.RealtimeNotificationPublisher;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryNotificationService {
    private final UserQueryApi userQueryApi;
    private final NotificationRepository notificationRepository;
    private final OutboxWriter outboxWriter;
    private final OutboxDispatcher outboxDispatcher;
    private final RealtimeNotificationPublisher realtimeNotificationPublisher;

    @Transactional
    public void saveOnInquiryAnswered(Long inquiryId, Long inquiryAuthorId) {
        UserInfo recipient = userQueryApi.getInfo(inquiryAuthorId);
        String title = NotificationTemplate.INQUIRY_ANSWER.title();
        String body = NotificationTemplate.INQUIRY_ANSWER.bodyTemplate();

        notificationRepository.save(
                Notification.create(recipient.userId(), null, null, title, AlarmType.INQUIRY_ANSWER, inquiryId));
        if (recipient.fcmToken() != null && recipient.eventAlarmEnabled()) {
            outboxWriter.write(
                    recipient.userId(),
                    recipient.fcmToken(),
                    title,
                    body,
                    NotificationPayload.inquiryOutbox(inquiryId));
        }
    }

    public void dispatchOnInquiryAnswered(Long inquiryId, Long inquiryAuthorId) {
        UserInfo recipient = userQueryApi.getInfo(inquiryAuthorId);
        if (!recipient.eventAlarmEnabled()) {
            return;
        }
        String title = NotificationTemplate.INQUIRY_ANSWER.title();
        String body = NotificationTemplate.INQUIRY_ANSWER.bodyTemplate();
        realtimeNotificationPublisher.publish(
                recipient.userId(), NotificationPayload.inquiryRealtime(inquiryId, title, body));
        outboxDispatcher.sendPendingOutboxImmediately(recipient.userId());
    }
}
