package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.out.dto.NotificationMessage;
import com.back.catchmate.notification.application.port.out.dto.NotificationSendResult;
import com.back.catchmate.notification.application.port.out.external.NotificationSenderPort;
import com.back.catchmate.notification.application.port.out.external.UserOnlineStatusFetchPort;
import com.back.catchmate.notification.domain.model.NotificationOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {

    @Mock
    private OutboxStateTransitioner outboxStateTransitioner;
    @Mock
    private NotificationSenderPort notificationSenderPort;
    @Mock
    private UserOnlineStatusFetchPort userOnlineStatusFetchPort;

    @Captor
    private ArgumentCaptor<Map<String, String>> dataCaptor;
    @Captor
    private ArgumentCaptor<List<NotificationMessage>> messagesCaptor;
    @Captor
    private ArgumentCaptor<List<NotificationOutbox>> successCaptor;
    @Captor
    private ArgumentCaptor<List<NotificationOutbox>> permanentCaptor;
    @Captor
    private ArgumentCaptor<List<NotificationOutbox>> retryableCaptor;

    private OutboxDispatcher sut;

    // objectMapper 는 실제 인스턴스로 둬야 parsePayload → data enrich 경로가 그대로 태워진다.
    @BeforeEach
    void setUp() {
        sut = new OutboxDispatcher(
                new ObjectMapper(), outboxStateTransitioner, notificationSenderPort, userOnlineStatusFetchPort);
    }

    @Test
    @DisplayName("스케줄러 배치 발송 시 FCM data 에 dedup 키(dedupKey=outbox.id)를 실어 보낸다")
    void processPendingNotifications_attachesDedupKey() {
        // given
        NotificationOutbox outbox = NotificationOutbox.builder()
                .id(123L)
                .recipientId(1L)
                .recipientAddress("token")
                .title("title")
                .body("body")
                .payload("{\"type\":\"ENROLL\"}")
                .build();
        given(outboxStateTransitioner.claimPendingNotifications(anyInt(), anyInt()))
                .willReturn(List.of(outbox));
        given(notificationSenderPort.sendNotifications(any()))
                .willReturn(List.of(NotificationSendResult.ofSuccess()));

        // when
        sut.processPendingNotifications();

        // then
        then(notificationSenderPort).should().sendNotifications(messagesCaptor.capture());
        assertThat(messagesCaptor.getValue()).hasSize(1);
        NotificationMessage sent = messagesCaptor.getValue().get(0);
        assertThat(sent.userId()).isEqualTo(1L);
        assertThat(sent.token()).isEqualTo("token");
        assertThat(sent.title()).isEqualTo("title");
        assertThat(sent.body()).isEqualTo("body");
        assertThat(sent.data()).containsEntry("dedupKey", "123"); // 재시도 간 불변인 dedup 키
        assertThat(sent.data()).containsEntry("type", "ENROLL");  // 원본 payload 는 보존
        assertThat(thenDispatchResults().success()).containsExactly(outbox);
    }

    @Test
    @DisplayName("즉시 발송(sendPendingOutboxImmediately) 시에도 FCM data 에 dedup 키를 실어 보낸다")
    void sendPendingOutboxImmediately_attachesDedupKey() {
        // given
        NotificationOutbox outbox = NotificationOutbox.builder()
                .id(456L)
                .recipientId(7L)
                .recipientAddress("token7")
                .title("title")
                .body("body")
                .payload("{}")
                .build();
        given(outboxStateTransitioner.claimPendingByRecipientId(7L))
                .willReturn(List.of(outbox));

        // when
        sut.sendPendingOutboxImmediately(7L);

        // then
        then(notificationSenderPort).should()
                .sendNotification(eq(7L), eq("token7"), any(), any(), dataCaptor.capture());
        assertThat(dataCaptor.getValue()).containsEntry("dedupKey", "456");
    }

    @Test
    @DisplayName("payload 파싱에 실패해도 dedup 키를 실어 정상 발송한다")
    void processPendingNotifications_attachesDedupKeyOnPayloadParseFailure() {
        // given - 파싱 실패 폴백 맵이 불변이면 dedup 키 주입에서 터진다
        NotificationOutbox outbox = NotificationOutbox.builder()
                .id(789L)
                .recipientId(2L)
                .recipientAddress("token2")
                .title("title")
                .body("body")
                .payload("not-a-json")
                .build();
        given(outboxStateTransitioner.claimPendingNotifications(anyInt(), anyInt()))
                .willReturn(List.of(outbox));
        given(notificationSenderPort.sendNotifications(any()))
                .willReturn(List.of(NotificationSendResult.ofSuccess()));

        // when
        sut.processPendingNotifications();

        // then
        then(notificationSenderPort).should().sendNotifications(messagesCaptor.capture());
        NotificationMessage sent = messagesCaptor.getValue().get(0);
        assertThat(sent.token()).isEqualTo("token2");
        assertThat(sent.data()).containsEntry("dedupKey", "789");
        assertThat(thenDispatchResults().success()).containsExactly(outbox);
    }

    @Test
    @DisplayName("배치 발송 결과를 성공/영구실패/재시도 세 갈래로 나눠 아웃박스에 확정한다")
    void processPendingNotifications_classifiesBatchResults() {
        // given - 한 배치 안에 성패가 섞여도 건별로 올바른 상태가 확정돼야 한다
        NotificationOutbox succeeded = outboxWithId(1L);
        NotificationOutbox permanentlyFailed = outboxWithId(2L);
        NotificationOutbox retryable = outboxWithId(3L);
        given(outboxStateTransitioner.claimPendingNotifications(anyInt(), anyInt()))
                .willReturn(List.of(succeeded, permanentlyFailed, retryable));
        given(notificationSenderPort.sendNotifications(any())).willReturn(List.of(
                NotificationSendResult.ofSuccess(),
                NotificationSendResult.ofPermanentFailure("토큰 만료"),
                NotificationSendResult.ofRetryableFailure("일시적 오류")
        ));

        // when
        sut.processPendingNotifications();

        // then
        DispatchResults results = thenDispatchResults();
        assertThat(results.success()).containsExactly(succeeded);
        assertThat(results.permanent()).containsExactly(permanentlyFailed);
        assertThat(results.retryable()).containsExactly(retryable);
    }

    private NotificationOutbox outboxWithId(Long id) {
        return NotificationOutbox.builder()
                .id(id)
                .recipientId(id)
                .recipientAddress("token" + id)
                .title("title")
                .body("body")
                .payload("{}")
                .build();
    }

    // applyDispatchResults 는 세 갈래 목록을 한 번에 받으므로, 캡처한 인자를 갈래별로 꺼내 쓰도록 묶는다.
    private DispatchResults thenDispatchResults() {
        then(outboxStateTransitioner).should().applyDispatchResults(
                successCaptor.capture(), permanentCaptor.capture(), retryableCaptor.capture(), any(), anyInt());
        return new DispatchResults(
                successCaptor.getValue(), permanentCaptor.getValue(), retryableCaptor.getValue());
    }

    private record DispatchResults(List<NotificationOutbox> success,
                                   List<NotificationOutbox> permanent,
                                   List<NotificationOutbox> retryable) {
    }
}
