package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.AdminInquiryNotificationDispatchUseCase;
import com.back.catchmate.notification.application.port.in.NotificationDispatchUseCase;
import com.back.catchmate.notification.application.port.in.OutboxDispatchUseCase;
import com.back.catchmate.notification.application.port.out.dto.NotificationUserInfo;
import com.back.catchmate.notification.application.port.out.external.UserFetchPort;
import com.back.catchmate.notification.domain.model.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 관리자 문의 답변 알림의 비동기 발송 전용 서비스(비트랜잭션).
 * FCM 호출 동안 DB 커넥션을 점유하지 않기 위해 {@link AdminInquiryNotificationService}(저장) 와 분리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInquiryNotificationDispatchService implements AdminInquiryNotificationDispatchUseCase {
    private static final String NOTIFICATION_TYPE = "INQUIRY";

    private final UserFetchPort userFetchPort;
    private final OutboxDispatchUseCase outboxDispatchUseCase;
    private final NotificationDispatchUseCase notificationDispatchUseCase;

    @Override
    public void dispatchOnInquiryAnswered(Long inquiryId, Long inquiryAuthorId) {
        NotificationUserInfo recipient = userFetchPort.getUser(inquiryAuthorId);
        if (!recipient.eventAlarmEnabled()) {
            return;
        }

        String title = NotificationTemplate.INQUIRY_ANSWER.getTitle();
        String body = NotificationTemplate.INQUIRY_ANSWER.getBodyTemplate();

        notificationDispatchUseCase.dispatch(
                recipient.userId(),
                Map.of(
                        "type", NOTIFICATION_TYPE,
                        "inquiryId", inquiryId.toString(),
                        "title", title,
                        "body", body
                )
        );

        outboxDispatchUseCase.sendPendingOutboxImmediately(recipient.userId());
    }
}
