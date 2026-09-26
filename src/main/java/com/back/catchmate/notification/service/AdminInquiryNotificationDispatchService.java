package com.back.catchmate.notification.service;

import com.back.catchmate.notification.entity.enums.NotificationTemplate;
import com.back.catchmate.notification.infra.RedisNotificationPublisher;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 관리자 문의 답변 알림의 비동기 발송 전용 서비스(비트랜잭션).
 * FCM 호출 동안 DB 커넥션을 점유하지 않기 위해 {@link AdminInquiryNotificationService}(저장) 와 분리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInquiryNotificationDispatchService {
    private static final String NOTIFICATION_TYPE = "INQUIRY";

    private final UserQueryApi userQueryApi;
    private final OutboxDispatcher outboxDispatcher;
    private final RedisNotificationPublisher redisNotificationPublisher;

    public void dispatchOnInquiryAnswered(Long inquiryId, Long inquiryAuthorId) {
        UserInfo recipient = userQueryApi.getInfo(inquiryAuthorId);
        if (!recipient.eventAlarmEnabled()) {
            return;
        }

        String title = NotificationTemplate.INQUIRY_ANSWER.getTitle();
        String body = NotificationTemplate.INQUIRY_ANSWER.getBodyTemplate();

        redisNotificationPublisher.dispatch(
                recipient.userId(),
                Map.of(
                        "type", NOTIFICATION_TYPE,
                        "inquiryId", inquiryId.toString(),
                        "title", title,
                        "body", body));

        outboxDispatcher.sendPendingOutboxImmediately(recipient.userId());
    }
}
