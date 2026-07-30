package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.AdminNoticeNotificationDispatchUseCase;
import com.back.catchmate.notification.application.port.in.NotificationDispatchUseCase;
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
 * 접속 중인 수신자에게 STOMP 실시간 알림만 보내고, FCM 은 {@code NotificationScheduler} 의 배치 회수에 맡긴다.
 * <p>
 * 다른 알림({@code Enroll} 등)과 달리 여기서 {@code sendPendingOutboxImmediately} 를 호출하지 <b>않는다</b>.
 * 그 메서드는 수신자가 1~2명인 알림을 위한 단건 경로라, 수신자 1명당 (Outbox claim 트랜잭션 + FCM 블로킹 호출 +
 * 상태 갱신 트랜잭션) 을 수행한다. 전체 유저를 대상으로 하는 공지에 쓰면 단일 발송 스레드가 그 과정을 유저 수만큼
 * 순차로 반복하며 수십 분간 점유되고, 그 사이 채팅·Enroll 알림이 같은 executor 를 두고 경쟁한다.
 * 공지는 초 단위 즉시성이 필요하지 않고 Outbox 가 at-least-once 를 보장하므로 스케줄러 배치에 위임한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNoticeNotificationDispatchService implements AdminNoticeNotificationDispatchUseCase {
    private static final String NOTIFICATION_TYPE = "NOTICE";

    private final UserFetchPort userFetchPort;
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

        // 전원이 같은 내용을 받으므로 수신자별 publish 대신 한 건으로 묶어 보낸다.
        List<Long> recipientIds = userFetchPort.getEventAlarmEnabledUsers().stream()
                .filter(NotificationUserInfo::eventAlarmEnabled)
                .map(NotificationUserInfo::userId)
                .toList();
        notificationDispatchUseCase.dispatchAll(recipientIds, stompPayload);
    }
}
