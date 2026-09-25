package com.back.catchmate.notification.service;

import com.back.catchmate.notification.dto.OutboxRecipient;
import com.back.catchmate.notification.entity.enums.AlarmType;
import com.back.catchmate.notification.entity.enums.NotificationTemplate;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdminNoticeNotificationService {
    private static final String NOTIFICATION_TYPE = "NOTICE";

    private final UserService userService;
    private final OutboxSaver outboxSaver;
    private final NotificationService notificationService;

    public void saveOnNoticeCreated(Long noticeId, String noticeTitle) {
        String title = NotificationTemplate.NOTICE_CREATED.getTitle();
        String body = NotificationTemplate.NOTICE_CREATED.formatBody(noticeTitle);
        List<UserSummary> recipients = userService.getEventAlarmEnabledUserSummaries();
        if (recipients.isEmpty()) return;

        // 인앱 알림함은 알림 설정과 무관하게 전원에게 남긴다(앱을 열었을 때 공지를 확인할 수 있어야 함).
        // 수신자 수만큼 단건 INSERT 를 반복하면 IDENTITY 탓에 그만큼 DB 왕복이 생기므로 배치로 적재한다.
        notificationService.createNotifications(
                recipients.stream().map(UserSummary::userId).toList(), null, null, title, AlarmType.EVENT, noticeId);

        // FCM 은 알림을 켜두고 토큰이 있는 수신자만 대상으로 한다.
        List<OutboxRecipient> outboxRecipients = recipients.stream()
                .filter(UserSummary::eventAlarmEnabled)
                .filter(recipient -> recipient.fcmToken() != null)
                .map(recipient -> new OutboxRecipient(recipient.userId(), recipient.fcmToken()))
                .toList();
        outboxSaver.saveOutboxBatch(
                outboxRecipients, title, body, Map.of("type", NOTIFICATION_TYPE, "noticeId", noticeId.toString()));

        log.info(
                "공지사항 알림 아웃박스 저장 완료: noticeId={}, recipientCount={}, outboxCount={}",
                noticeId,
                recipients.size(),
                outboxRecipients.size());
    }
}
