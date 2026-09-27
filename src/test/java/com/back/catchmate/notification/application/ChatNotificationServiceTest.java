package com.back.catchmate.notification.application;

import static com.back.catchmate.notification.fixture.NotificationFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.back.catchmate.chat.application.ChatQueryApi;
import com.back.catchmate.chat.application.dto.api.ChatRecipientInfo;
import com.back.catchmate.notification.domain.NotificationPayload;
import com.back.catchmate.notification.domain.OutboxRecipient;
import com.back.catchmate.notification.domain.RealtimeNotificationPublisher;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatNotificationServiceTest {

    private static final Long ROOM_ID = 50L;
    private static final Long SENDER_ID = 1L;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private ChatQueryApi chatQueryApi;

    @Mock
    private OutboxWriter outboxWriter;

    @Mock
    private OutboxDispatcher outboxDispatcher;

    @Mock
    private RealtimeNotificationPublisher realtimeNotificationPublisher;

    @InjectMocks
    private ChatNotificationService service;

    @Test
    @DisplayName("저장: 방 알림·채팅 알림이 켜져 있고 토큰이 있으며 그 방을 보고 있지 않은 수신자만 아웃박스에 쌓는다")
    @SuppressWarnings("unchecked")
    void savesOnlyEligibleRecipients() {
        // given — 2 대상, 3 방 알림 끔, 4 채팅 알림 끔, 5 토큰 없음, 6 그 방을 보는 중
        given(chatQueryApi.getRecipients(ROOM_ID, SENDER_ID))
                .willReturn(List.of(
                        new ChatRecipientInfo(2L, true),
                        new ChatRecipientInfo(3L, false),
                        new ChatRecipientInfo(4L, true),
                        new ChatRecipientInfo(5L, true),
                        new ChatRecipientInfo(6L, true)));
        given(userQueryApi.getInfo(SENDER_ID)).willReturn(user(SENDER_ID, "철수", null, true, true, true));
        Map<Long, UserInfo> recipients = new LinkedHashMap<>();
        recipients.put(2L, user(2L, "a", "t2", true, true, true));
        recipients.put(3L, user(3L, "b", "t3", true, true, true));
        recipients.put(4L, user(4L, "c", "t4", false, true, true));
        recipients.put(5L, user(5L, "d", null, true, true, true));
        recipients.put(6L, user(6L, "e", "t6", true, true, true));
        given(userQueryApi.getInfos(List.of(2L, 3L, 4L, 5L, 6L))).willReturn(recipients);
        given(chatQueryApi.getFocusRooms(List.of(2L, 6L))).willReturn(Map.of(6L, ROOM_ID));

        // when
        service.saveOnChatMessageSent(ROOM_ID, 900L, SENDER_ID, "안녕");

        // then
        ArgumentCaptor<List<OutboxRecipient>> captor = ArgumentCaptor.forClass(List.class);
        then(outboxWriter)
                .should()
                .writeAll(
                        captor.capture(),
                        eq("철수"),
                        eq("안녕"),
                        eq(NotificationPayload.chat(ROOM_ID, SENDER_ID, "철수", "안녕", "철수", "안녕")));
        assertThat(captor.getValue()).containsExactly(new OutboxRecipient(2L, "t2"));
    }

    @Test
    @DisplayName("발송: 그 방을 보지 않는 수신자 전원에게 실시간 알림, 알림을 켠 사람만 즉시 푸시")
    void dispatchesRealtimeAndPush() {
        // given — 2 모두 켬, 3 방 알림 끔, 6 그 방을 보는 중
        given(chatQueryApi.getRecipients(ROOM_ID, SENDER_ID))
                .willReturn(List.of(
                        new ChatRecipientInfo(2L, true),
                        new ChatRecipientInfo(3L, false),
                        new ChatRecipientInfo(6L, true)));
        given(userQueryApi.getInfo(SENDER_ID)).willReturn(user(SENDER_ID, "철수", null, true, true, true));
        Map<Long, UserInfo> recipients = new LinkedHashMap<>();
        recipients.put(2L, user(2L, "a", "t2", true, true, true));
        recipients.put(3L, user(3L, "b", "t3", true, true, true));
        recipients.put(6L, user(6L, "e", "t6", true, true, true));
        given(userQueryApi.getInfos(List.of(2L, 3L, 6L))).willReturn(recipients);
        given(chatQueryApi.getFocusRooms(List.of(2L, 3L, 6L))).willReturn(Map.of(6L, ROOM_ID));

        // when
        service.dispatchOnChatMessageSent(ROOM_ID, 900L, SENDER_ID, "안녕");

        // then
        then(realtimeNotificationPublisher)
                .should()
                .publishAll(List.of(2L, 3L), NotificationPayload.chat(ROOM_ID, SENDER_ID, "철수", "안녕", "철수", "안녕"));
        then(outboxDispatcher).should().sendPendingOutboxesImmediately(List.of(2L));
        then(outboxDispatcher).should(never()).sendPendingOutboxImmediately(anyLong());
    }

    @Test
    @DisplayName("수신자가 없으면 발신자 조회도 하지 않는다")
    void noRecipients() {
        given(chatQueryApi.getRecipients(ROOM_ID, SENDER_ID)).willReturn(List.of());

        service.saveOnChatMessageSent(ROOM_ID, 900L, SENDER_ID, "안녕");
        service.dispatchOnChatMessageSent(ROOM_ID, 900L, SENDER_ID, "안녕");

        then(userQueryApi).shouldHaveNoInteractions();
        then(outboxDispatcher).shouldHaveNoInteractions();
    }
}
