package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.AdminInquiryNotificationUseCase;
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

import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class
AdminInquiryNotificationService implements AdminInquiryNotificationUseCase {
    private static final String NOTIFICATION_TYPE = "INQUIRY";

    private final UserFetchPort userFetchPort;
    private final OutboxSaveUseCase outboxSaveUseCase;
    private final NotificationInternalCommandUseCase notificationInternalCommandUseCase;

    @Override
    public void saveOnInquiryAnswered(Long inquiryId, Long inquiryAuthorId) {
        NotificationUserInfo recipient = userFetchPort.getUser(inquiryAuthorId);
        String title = NotificationTemplate.INQUIRY_ANSWER.getTitle();
        String body = NotificationTemplate.INQUIRY_ANSWER.getBodyTemplate();

        notificationInternalCommandUseCase.createNotification(
                recipient.userId(),
                null,
                null,
                title,
                AlarmType.INQUIRY_ANSWER,
                inquiryId
        );

        if (recipient.fcmToken() != null && recipient.eventAlarmEnabled()) {
            outboxSaveUseCase.saveOutbox(
                    recipient.userId(),
                    recipient.fcmToken(),
                    title,
                    body,
                    Map.of(
                            "type", NOTIFICATION_TYPE,
                            "inquiryId", inquiryId.toString()
                    )
            );
            log.info("관리자 답변 알림 아웃박스 저장 완료: recipientId: {}", recipient.userId());
        }
    }
}
