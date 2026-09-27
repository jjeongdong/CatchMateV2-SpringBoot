package com.back.catchmate.notification.application;

import com.back.catchmate.notification.domain.AlarmType;
import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.NotificationPayload;
import com.back.catchmate.notification.domain.NotificationRepository;
import com.back.catchmate.notification.domain.NotificationTemplate;
import com.back.catchmate.notification.domain.OutboxRecipient;
import com.back.catchmate.notification.domain.RealtimeNotificationPublisher;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공지 알림.
 * <p>
 * 다른 알림과 달리 발송 때 {@code sendPendingOutboxImmediately} 를 부르지 <b>않는다</b>.
 * 그 메서드는 수신자 1명당 (선점 트랜잭션 + FCM 블로킹 호출 + 상태 갱신 트랜잭션) 을 수행하는 단건 경로라,
 * 전체 유저 대상 공지에 쓰면 발송 스레드가 유저 수만큼 순차 반복하며 수십 분간 점유되고 그 사이
 * 채팅·신청 알림이 같은 executor 를 두고 경쟁한다. 공지는 초 단위 즉시성이 필요 없고 아웃박스가
 * at-least-once 를 보장하므로 스케줄러 배치에 맡긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeNotificationService {
    private final UserQueryApi userQueryApi;
    private final NotificationRepository notificationRepository;
    private final OutboxWriter outboxWriter;
    private final RealtimeNotificationPublisher realtimeNotificationPublisher;

    @Transactional
    public void saveOnNoticeCreated(Long noticeId, String noticeTitle) {
        List<UserInfo> recipients = userQueryApi.getEventAlarmEnabledInfos();
        if (recipients.isEmpty()) {
            return;
        }
        String title = NotificationTemplate.NOTICE_CREATED.title();
        String body = NotificationTemplate.NOTICE_CREATED.formatBody(noticeTitle);

        // 인앱 알림은 이벤트 알림을 켠 사람에게만 남는다 (옛 동작 — spec §8 부채).
        // 수신자 수만큼 단건 INSERT 를 반복하면 IDENTITY 탓에 그만큼 DB 왕복이 생기므로 배치로 적재한다.
        notificationRepository.saveAllInBatch(recipients.stream()
                .map(recipient -> Notification.create(recipient.userId(), null, null, title, AlarmType.EVENT, noticeId))
                .toList());

        List<OutboxRecipient> outboxRecipients = recipients.stream()
                .filter(UserInfo::eventAlarmEnabled)
                .filter(recipient -> recipient.fcmToken() != null)
                .map(recipient -> new OutboxRecipient(recipient.userId(), recipient.fcmToken()))
                .toList();
        outboxWriter.writeAll(outboxRecipients, title, body, NotificationPayload.noticeOutbox(noticeId));

        log.info(
                "공지 알림 적재 완료 noticeId={}, recipientCount={}, outboxCount={}",
                noticeId,
                recipients.size(),
                outboxRecipients.size());
    }

    public void dispatchOnNoticeCreated(Long noticeId, String noticeTitle) {
        String title = NotificationTemplate.NOTICE_CREATED.title();
        String body = NotificationTemplate.NOTICE_CREATED.formatBody(noticeTitle);
        List<Long> recipientIds = userQueryApi.getEventAlarmEnabledInfos().stream()
                .filter(UserInfo::eventAlarmEnabled)
                .map(UserInfo::userId)
                .toList();
        // 전원이 같은 내용을 받으므로 수신자별 publish 대신 묶어 보낸다.
        realtimeNotificationPublisher.publishAll(
                recipientIds, NotificationPayload.noticeRealtime(noticeId, title, body));
    }
}
