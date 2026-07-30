package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.AdminNoticeNotificationUseCase;
import com.back.catchmate.notification.application.port.in.NotificationInternalCommandUseCase;
import com.back.catchmate.notification.application.port.in.OutboxRecipient;
import com.back.catchmate.notification.application.port.in.OutboxSaveUseCase;
import com.back.catchmate.notification.application.port.out.dto.NotificationUserInfo;
import com.back.catchmate.notification.application.port.out.external.UserFetchPort;
import com.back.catchmate.notification.domain.model.AlarmType;
import com.back.catchmate.notification.domain.model.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdminNoticeNotificationService implements AdminNoticeNotificationUseCase {
    private static final String NOTIFICATION_TYPE = "NOTICE";

    private final UserFetchPort userFetchPort;
    private final OutboxSaveUseCase outboxSaveUseCase;
    private final NotificationInternalCommandUseCase notificationInternalCommandUseCase;

    @Override
    public void saveOnNoticeCreated(Long noticeId, String noticeTitle) {
        String title = NotificationTemplate.NOTICE_CREATED.getTitle();
        String body = NotificationTemplate.NOTICE_CREATED.formatBody(noticeTitle);
        List<NotificationUserInfo> recipients = userFetchPort.getEventAlarmEnabledUsers();
        if (recipients.isEmpty()) return;

        // 인앱 알림함은 알림 설정과 무관하게 전원에게 남긴다(앱을 열었을 때 공지를 확인할 수 있어야 함).
        // 수신자 수만큼 단건 INSERT 를 반복하면 IDENTITY 탓에 그만큼 DB 왕복이 생기므로 배치로 적재한다.
        notificationInternalCommandUseCase.createNotifications(
                recipients.stream().map(NotificationUserInfo::userId).toList(),
                null,
                null,
                title,
                AlarmType.EVENT,
                noticeId
        );

        // FCM 은 알림을 켜두고 토큰이 있는 수신자만 대상으로 한다.
        List<OutboxRecipient> outboxRecipients = recipients.stream()
                .filter(NotificationUserInfo::eventAlarmEnabled)
                .filter(recipient -> recipient.fcmToken() != null)
                .map(recipient -> new OutboxRecipient(recipient.userId(), recipient.fcmToken()))
                .toList();
        outboxSaveUseCase.saveOutboxBatch(
                outboxRecipients,
                title,
                body,
                Map.of(
                        "type", NOTIFICATION_TYPE,
                        "noticeId", noticeId.toString()
                )
        );

        log.info("공지사항 알림 아웃박스 저장 완료: noticeId={}, recipientCount={}, outboxCount={}",
                noticeId, recipients.size(), outboxRecipients.size());
    }
}
