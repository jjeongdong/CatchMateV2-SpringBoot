package com.back.catchmate.notification.service;

import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import com.back.catchmate.notification.entity.enums.AlarmType;
import com.back.catchmate.notification.entity.enums.NotificationTemplate;
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
AdminInquiryNotificationService {
    private static final String NOTIFICATION_TYPE = "INQUIRY";

    private final UserService userService;
    private final OutboxSaver outboxSaver;
    private final NotificationService notificationService;

    public void saveOnInquiryAnswered(Long inquiryId, Long inquiryAuthorId) {
        UserSummary recipient = userService.getUserSummary(inquiryAuthorId);
        String title = NotificationTemplate.INQUIRY_ANSWER.getTitle();
        String body = NotificationTemplate.INQUIRY_ANSWER.getBodyTemplate();

        notificationService.createNotification(
                recipient.userId(),
                null,
                null,
                title,
                AlarmType.INQUIRY_ANSWER,
                inquiryId
        );

        if (recipient.fcmToken() != null && recipient.eventAlarmEnabled()) {
            outboxSaver.saveOutbox(
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
