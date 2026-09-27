package com.back.catchmate.notification.application;

import static com.back.catchmate.notification.fixture.NotificationFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.notification.domain.AlarmType;
import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.NotificationPayload;
import com.back.catchmate.notification.domain.NotificationRepository;
import com.back.catchmate.notification.domain.RealtimeNotificationPublisher;
import com.back.catchmate.user.application.UserQueryApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InquiryNotificationServiceTest {

    private static final String TITLE = "1:1 문의 답변 완료";
    private static final String BODY = "작성하신 1:1 문의에 답변이 등록되었습니다.";

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OutboxWriter outboxWriter;

    @Mock
    private OutboxDispatcher outboxDispatcher;

    @Mock
    private RealtimeNotificationPublisher realtimeNotificationPublisher;

    @InjectMocks
    private InquiryNotificationService service;

    @Test
    @DisplayName("저장: 작성자에게 문의 답변 알림을 남기고 이벤트 알림을 켰으면 아웃박스도 쌓는다")
    void saves() {
        given(userQueryApi.getInfo(1L)).willReturn(user(1L, "철수", "token-1", true, true, true));

        service.saveOnInquiryAnswered(3L, 1L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        then(notificationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(AlarmType.INQUIRY_ANSWER);
        assertThat(captor.getValue().getTargetId()).isEqualTo(3L);
        assertThat(captor.getValue().getSenderId()).isNull();
        then(outboxWriter).should().write(1L, "token-1", TITLE, BODY, NotificationPayload.inquiryOutbox(3L));
    }

    @Test
    @DisplayName("발송: 이벤트 알림을 꺼 두면 보내지 않고, 켜 두면 실시간 알림 + 즉시 푸시")
    void dispatches() {
        given(userQueryApi.getInfo(1L)).willReturn(user(1L, "철수", "token-1", true, true, false));
        given(userQueryApi.getInfo(2L)).willReturn(user(2L, "영희", "token-2", true, true, true));

        service.dispatchOnInquiryAnswered(3L, 1L);
        service.dispatchOnInquiryAnswered(4L, 2L);

        then(realtimeNotificationPublisher).should().publish(2L, NotificationPayload.inquiryRealtime(4L, TITLE, BODY));
        then(realtimeNotificationPublisher).shouldHaveNoMoreInteractions();
        then(outboxDispatcher).should().sendPendingOutboxImmediately(2L);
        then(outboxDispatcher).shouldHaveNoMoreInteractions();
    }
}
