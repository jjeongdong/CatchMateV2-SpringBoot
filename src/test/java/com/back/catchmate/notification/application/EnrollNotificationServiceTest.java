package com.back.catchmate.notification.application;

import static com.back.catchmate.notification.fixture.NotificationFixture.board;
import static com.back.catchmate.notification.fixture.NotificationFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.back.catchmate.board.application.BoardQueryApi;
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
class EnrollNotificationServiceTest {

    private static final Long ENROLL_ID = 100L;
    private static final Long BOARD_ID = 10L;
    private static final Long APPLICANT_ID = 1L;
    private static final Long OWNER_ID = 2L;

    @Mock
    private BoardQueryApi boardQueryApi;

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
    private EnrollNotificationService service;

    @Test
    @DisplayName("신청: 작성자에게 인앱 알림을 남기고, 알림을 켜고 토큰이 있으면 아웃박스도 쌓는다")
    void savesRequested() {
        // given
        given(userQueryApi.getInfo(APPLICANT_ID)).willReturn(user(APPLICANT_ID, "철수", null, true, true, true));
        given(userQueryApi.getInfo(OWNER_ID)).willReturn(user(OWNER_ID, "영희", "token-2", true, true, true));
        given(boardQueryApi.getInfo(BOARD_ID)).willReturn(board(BOARD_ID, "직관 가요", 20L));

        // when
        service.saveOnEnrollRequested(ENROLL_ID, BOARD_ID, APPLICANT_ID, OWNER_ID);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        then(notificationRepository).should().save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(OWNER_ID);
        assertThat(saved.getSenderId()).isEqualTo(APPLICANT_ID);
        assertThat(saved.getType()).isEqualTo(AlarmType.ENROLL);
        assertThat(saved.getTargetId()).isEqualTo(ENROLL_ID);
        assertThat(saved.getTitle()).isEqualTo("철수님이 참여 신청을 보냈습니다");
        String body = "'직관 가요' 모임에 새로운 참여 신청이 도착했습니다.";
        then(outboxWriter)
                .should()
                .write(
                        OWNER_ID,
                        "token-2",
                        saved.getTitle(),
                        body,
                        NotificationPayload.enroll(
                                NotificationPayload.TYPE_ENROLL_REQUEST, BOARD_ID, saved.getTitle(), body));
    }

    @Test
    @DisplayName("수락: 신청자가 알림을 꺼 두면 인앱 알림만 남긴다")
    void savesAcceptedWithoutOutboxWhenAlarmOff() {
        given(boardQueryApi.getInfo(BOARD_ID)).willReturn(board(BOARD_ID, "직관 가요", 20L));
        given(userQueryApi.getInfo(APPLICANT_ID)).willReturn(user(APPLICANT_ID, "철수", "token-1", true, false, true));

        service.saveOnEnrollAccepted(ENROLL_ID, BOARD_ID, APPLICANT_ID, OWNER_ID);

        then(notificationRepository).should().save(any(Notification.class));
        then(outboxWriter).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("취소: 인앱 알림만 남기고 푸시는 보내지 않는다")
    void savesCancelledWithoutOutbox() {
        given(userQueryApi.getInfo(APPLICANT_ID)).willReturn(user(APPLICANT_ID, "철수", null, true, true, true));

        service.saveOnEnrollCancelled(ENROLL_ID, BOARD_ID, APPLICANT_ID, OWNER_ID);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        then(notificationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(OWNER_ID);
        assertThat(captor.getValue().getTitle()).isEqualTo("철수님이 참여 신청을 취소했습니다");
        then(outboxWriter).shouldHaveNoInteractions();
        then(boardQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("발송: 수신자가 신청 알림을 꺼 두면 아무것도 보내지 않는다")
    void dispatchSkipsWhenAlarmOff() {
        given(userQueryApi.getInfo(OWNER_ID)).willReturn(user(OWNER_ID, "영희", "token-2", true, false, true));

        service.dispatchOnEnrollRequested(ENROLL_ID, BOARD_ID, APPLICANT_ID, OWNER_ID);

        then(realtimeNotificationPublisher).shouldHaveNoInteractions();
        then(outboxDispatcher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("발송: 수락은 실시간 알림 + 즉시 푸시, 취소는 실시간 알림만")
    void dispatchAcceptedAndCancelled() {
        given(userQueryApi.getInfo(APPLICANT_ID)).willReturn(user(APPLICANT_ID, "철수", "token-1", true, true, true));
        given(userQueryApi.getInfo(OWNER_ID)).willReturn(user(OWNER_ID, "영희", "token-2", true, true, true));
        given(boardQueryApi.getInfo(BOARD_ID)).willReturn(board(BOARD_ID, "직관 가요", 20L));

        service.dispatchOnEnrollAccepted(ENROLL_ID, BOARD_ID, APPLICANT_ID, OWNER_ID);
        service.dispatchOnEnrollCancelled(ENROLL_ID, BOARD_ID, APPLICANT_ID, OWNER_ID);

        then(realtimeNotificationPublisher).should(times(2)).publish(any(), any());
        then(outboxDispatcher).should().sendPendingOutboxImmediately(APPLICANT_ID);
        then(outboxDispatcher).should(never()).sendPendingOutboxImmediately(OWNER_ID);
    }
}
