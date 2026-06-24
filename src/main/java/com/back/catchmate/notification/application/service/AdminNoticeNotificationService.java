package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.AdminNoticeNotificationUseCase;
import com.back.catchmate.notification.application.port.in.NotificationInternalCommandUseCase;
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

        for (NotificationUserInfo recipient : recipients) {
            notificationInternalCommandUseCase.createNotification(
                    recipient.userId(),
                    null,
                    null,
                    title,
                    AlarmType.EVENT,
                    noticeId
            );

            if (!recipient.eventAlarmEnabled()) continue;
            if (recipient.fcmToken() == null) continue;
            outboxSaveUseCase.saveOutbox(
                    recipient.userId(),
                    recipient.fcmToken(),
                    title,
                    body,
                    Map.of(
                            "type", NOTIFICATION_TYPE,
                            "noticeId", noticeId.toString()
                    )
            );
        }
        log.info("공지사항 알림 아웃박스 저장 완료: noticeId={}, recipientCount={}", noticeId, recipients.size());
    }
}
