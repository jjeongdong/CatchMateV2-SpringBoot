package com.back.catchmate.notification.application;

import static com.back.catchmate.notification.fixture.NotificationFixture.outbox;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.back.catchmate.chat.application.ChatQueryApi;
import com.back.catchmate.notification.domain.NotificationOutbox;
import com.back.catchmate.notification.domain.PushMessage;
import com.back.catchmate.notification.domain.PushOutcome;
import com.back.catchmate.notification.domain.PushSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {

    private static final String CHAT_IN_ROOM_50 = "{\"type\":\"CHAT\",\"roomId\":\"50\"}";
    private static final String ENROLL = "{\"type\":\"ENROLL_REQUEST\",\"boardId\":\"10\"}";

    @Mock
    private OutboxStateTransitioner transitioner;

    @Mock
    private PushSender pushSender;

    @Mock
    private ChatQueryApi chatQueryApi;

    private OutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new OutboxDispatcher(new ObjectMapper(), transitioner, pushSender, chatQueryApi);
        ReflectionTestUtils.setField(dispatcher, "maxRetryCount", 5);
        ReflectionTestUtils.setField(dispatcher, "batchSize", 500);
        ReflectionTestUtils.setField(dispatcher, "processingTimeoutSeconds", 300);
    }

    @Test
    @DisplayName("배치: 그 채팅방을 보고 있는 수신자는 푸시 없이 성공 처리하고, 나머지는 dedupKey 를 실어 보낸다")
    @SuppressWarnings("unchecked")
    void batchSkipsFocusedChatRoom() {
        // given
        NotificationOutbox focused = outbox(1L, 2L, CHAT_IN_ROOM_50);
        NotificationOutbox enroll = outbox(2L, 3L, ENROLL);
        given(transitioner.claimPendingNotifications(5, 500)).willReturn(List.of(focused, enroll));
        given(chatQueryApi.getFocusRooms(List.of(2L))).willReturn(Map.of(2L, 50L));
        given(pushSender.sendAll(any())).willReturn(List.of(PushOutcome.ofRetryableFailure("일시 장애")));

        // when
        dispatcher.processPendingNotifications();

        // then
        ArgumentCaptor<List<PushMessage>> messages = ArgumentCaptor.forClass(List.class);
        then(pushSender).should().sendAll(messages.capture());
        assertThat(messages.getValue()).singleElement().satisfies(message -> {
            assertThat(message.userId()).isEqualTo(3L);
            assertThat(message.data()).containsEntry("dedupKey", "2");
        });
        then(transitioner)
                .should()
                .applyDispatchResults(List.of(focused), List.of(), List.of(enroll), Map.of(2L, "일시 장애"), 5);
    }

    @Test
    @DisplayName("배치: 영구 실패는 재시도 대상과 따로 분류한다")
    void batchClassifiesPermanentFailure() {
        NotificationOutbox enroll = outbox(2L, 3L, ENROLL);
        given(transitioner.claimPendingNotifications(5, 500)).willReturn(List.of(enroll));
        given(pushSender.sendAll(any())).willReturn(List.of(PushOutcome.ofPermanentFailure("토큰 만료")));

        dispatcher.processPendingNotifications();

        then(transitioner).should().applyDispatchResults(List.of(), List.of(enroll), List.of(), Map.of(2L, "토큰 만료"), 5);
        then(chatQueryApi).should(never()).getFocusRooms(any());
    }

    @Test
    @DisplayName("배치 도중 예외(포커스 방 조회 장애)가 나면 선점한 행 전체를 재시도 대상으로 되돌린다")
    void rollsBackClaimWhenBatchFails() {
        NotificationOutbox chat = outbox(1L, 2L, CHAT_IN_ROOM_50);
        given(transitioner.claimPendingNotifications(5, 500)).willReturn(List.of(chat));
        given(chatQueryApi.getFocusRooms(List.of(2L))).willThrow(new IllegalStateException("Redis 다운"));

        dispatcher.processPendingNotifications();

        then(transitioner)
                .should()
                .applyDispatchResults(List.of(), List.of(), List.of(chat), Map.of(1L, "배치 처리 실패 - Redis 다운"), 5);
        then(pushSender).should(never()).sendAll(any());
    }

    @Test
    @DisplayName("즉시 발송: 채팅방을 보고 있으면 푸시 없이 성공 처리")
    void singleSkipsFocusedChatRoom() {
        NotificationOutbox chat = outbox(1L, 2L, CHAT_IN_ROOM_50);
        given(transitioner.claimPendingByRecipientId(2L)).willReturn(List.of(chat));
        given(chatQueryApi.findFocusRoom(2L)).willReturn(Optional.of(50L));

        dispatcher.sendPendingOutboxImmediately(2L);

        then(transitioner).should().updateStatusSuccess(chat);
        then(pushSender).should(never()).send(any());
    }

    @Test
    @DisplayName("즉시 발송: 결과에 따라 성공·영구 실패·재시도로 확정한다")
    void singleAppliesResult() {
        NotificationOutbox first = outbox(1L, 3L, ENROLL);
        NotificationOutbox second = outbox(2L, 3L, ENROLL);
        NotificationOutbox third = outbox(3L, 3L, ENROLL);
        given(transitioner.claimPendingByRecipientId(3L)).willReturn(List.of(first, second, third));
        given(pushSender.send(any()))
                .willReturn(
                        PushOutcome.ofSuccess(),
                        PushOutcome.ofPermanentFailure("토큰 만료"),
                        PushOutcome.ofRetryableFailure("일시 장애"));

        dispatcher.sendPendingOutboxImmediately(3L);

        then(transitioner).should().updateStatusSuccess(first);
        then(transitioner).should().updateStatusPermanentFailure(second, "토큰 만료");
        then(transitioner).should().updateStatusFailure(third, 5, "일시 장애");
    }

    @Test
    @DisplayName("즉시 발송: 푸시 전에 실패해도(포커스 방 조회 장애) 재시도 대상으로 되돌린다")
    void singleSendFailsBeforePush() {
        NotificationOutbox chat = outbox(1L, 2L, CHAT_IN_ROOM_50);
        given(transitioner.claimPendingByRecipientId(2L)).willReturn(List.of(chat));
        given(chatQueryApi.findFocusRoom(2L)).willThrow(new IllegalStateException("Redis 다운"));

        dispatcher.sendPendingOutboxImmediately(2L);

        then(transitioner).should().updateStatusFailure(eq(chat), eq(5), anyString());
        then(pushSender).should(never()).send(any());
    }
}
