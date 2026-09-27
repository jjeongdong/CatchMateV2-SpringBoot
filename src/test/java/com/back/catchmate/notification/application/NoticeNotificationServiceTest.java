package com.back.catchmate.notification.application;

import static com.back.catchmate.notification.fixture.NotificationFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.notification.domain.AlarmType;
import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.NotificationPayload;
import com.back.catchmate.notification.domain.NotificationRepository;
import com.back.catchmate.notification.domain.OutboxRecipient;
import com.back.catchmate.notification.domain.RealtimeNotificationPublisher;
import com.back.catchmate.user.application.UserQueryApi;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeNotificationServiceTest {

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OutboxWriter outboxWriter;

    @Mock
    private RealtimeNotificationPublisher realtimeNotificationPublisher;

    @InjectMocks
    private NoticeNotificationService service;

    @Test
    @DisplayName("저장: 이벤트 알림을 켠 사람 전원에게 인앱 알림을 배치로 남기고, 토큰이 있는 사람만 아웃박스에 쌓는다")
    @SuppressWarnings("unchecked")
    void saves() {
        given(userQueryApi.getEventAlarmEnabledInfos())
                .willReturn(List.of(user(1L, "a", "t1", true, true, true), user(2L, "b", null, true, true, true)));

        service.saveOnNoticeCreated(4L, "점검 안내");

        ArgumentCaptor<List<Notification>> notifications = ArgumentCaptor.forClass(List.class);
        then(notificationRepository).should().saveAllInBatch(notifications.capture());
        assertThat(notifications.getValue()).extracting(Notification::getUserId).containsExactly(1L, 2L);
        assertThat(notifications.getValue()).allSatisfy(notification -> {
            assertThat(notification.getType()).isEqualTo(AlarmType.EVENT);
            assertThat(notification.getTargetId()).isEqualTo(4L);
        });
        then(outboxWriter)
                .should()
                .writeAll(
                        List.of(new OutboxRecipient(1L, "t1")),
                        "새 공지사항",
                        "점검 안내",
                        NotificationPayload.noticeOutbox(4L));
    }

    @Test
    @DisplayName("저장: 대상이 없으면 아무것도 하지 않는다")
    void savesNothingWithoutRecipients() {
        given(userQueryApi.getEventAlarmEnabledInfos()).willReturn(List.of());

        service.saveOnNoticeCreated(4L, "점검 안내");

        then(notificationRepository).shouldHaveNoInteractions();
        then(outboxWriter).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("발송: 실시간 알림을 한 번에 묶어 보내고, 푸시는 스케줄러 배치에 맡긴다")
    void dispatchesRealtimeOnly() {
        given(userQueryApi.getEventAlarmEnabledInfos())
                .willReturn(List.of(user(1L, "a", "t1", true, true, true), user(2L, "b", null, true, true, true)));

        service.dispatchOnNoticeCreated(4L, "점검 안내");

        then(realtimeNotificationPublisher)
                .should()
                .publishAll(List.of(1L, 2L), NotificationPayload.noticeRealtime(4L, "새 공지사항", "점검 안내"));
    }
}
