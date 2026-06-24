package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.AdminNoticeNotificationDispatchUseCase;
import com.back.catchmate.notification.application.port.in.NotificationDispatchUseCase;
import com.back.catchmate.notification.application.port.in.OutboxDispatchUseCase;
import com.back.catchmate.notification.application.port.out.dto.NotificationUserInfo;
import com.back.catchmate.notification.application.port.out.external.UserFetchPort;
import com.back.catchmate.notification.domain.model.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 공지사항 알림의 비동기 발송 전용 서비스(비트랜잭션).
 * FCM 호출 동안 DB 커넥션을 점유하지 않기 위해 {@link AdminNoticeNotificationService}(저장) 와 분리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNoticeNotificationDispatchService implements AdminNoticeNotificationDispatchUseCase {
    private static final String NOTIFICATION_TYPE = "NOTICE";

    private final UserFetchPort userFetchPort;
    private final OutboxDispatchUseCase outboxDispatchUseCase;
    private final NotificationDispatchUseCase notificationDispatchUseCase;

    @Override
    public void dispatchOnNoticeCreated(Long noticeId, String noticeTitle) {
        String title = NotificationTemplate.NOTICE_CREATED.getTitle();
        String body = NotificationTemplate.NOTICE_CREATED.formatBody(noticeTitle);
        Map<String, String> stompPayload = Map.of(
                "type", NOTIFICATION_TYPE,
                "noticeId", noticeId.toString(),
                "title", title,
                "body", body
        );

        List<NotificationUserInfo> recipients = userFetchPort.getEventAlarmEnabledUsers();
        for (NotificationUserInfo recipient : recipients) {
            if (!recipient.eventAlarmEnabled()) continue;
            notificationDispatchUseCase.dispatch(recipient.userId(), stompPayload);
            if (recipient.fcmToken() != null) {
                outboxDispatchUseCase.sendPendingOutboxImmediately(recipient.userId());
            }
        }
    }
}
