package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.NotificationDispatchUseCase;
import com.back.catchmate.notification.application.port.in.OutboxDispatchUseCase;
import com.back.catchmate.notification.application.port.out.dto.NotificationChatRecipientInfo;
import com.back.catchmate.notification.application.port.out.dto.NotificationUserInfo;
import com.back.catchmate.notification.application.port.out.external.ChatRoomFetchPort;
import com.back.catchmate.notification.application.port.out.external.UserFetchPort;
import com.back.catchmate.notification.application.port.out.external.UserOnlineStatusFetchPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ChatNotificationDispatchServiceTest {

    private static final Long ROOM_ID = 7L;
    private static final Long SENDER_ID = 1L;

    @Mock
    private UserFetchPort userFetchPort;

    @Mock
    private ChatRoomFetchPort chatRoomFetchPort;

    @Mock
    private UserOnlineStatusFetchPort userOnlineStatusFetchPort;

    @Mock
    private OutboxDispatchUseCase outboxDispatchUseCase;

    @Mock
    private NotificationDispatchUseCase notificationDispatchUseCase;

    @InjectMocks
    private ChatNotificationDispatchService sut;

    @Test
    @DisplayName("알림 설정과 무관하게 수신자 전원의 포커스 방을 한 번의 배치로 조회한다")
    void 수신자_전원을_한_번의_배치로_조회한다() {
        // given: 저장 단계와 달리 알림 OFF 인 수신자도 STOMP 동기화 대상이라 조회에서 빠지면 안 된다
        given(chatRoomFetchPort.getChatRoomRecipients(ROOM_ID, SENDER_ID)).willReturn(List.of(
                new NotificationChatRecipientInfo(2L, true),
                new NotificationChatRecipientInfo(3L, false)
        ));
        given(userFetchPort.getUser(SENDER_ID)).willReturn(user(SENDER_ID, "발신자", true));
        given(userFetchPort.getUsers(anyList())).willReturn(List.of(
                user(2L, "수신자2", true),
                user(3L, "수신자3", false)
        ));
        given(userOnlineStatusFetchPort.getUserFocusRooms(anyList())).willReturn(Map.of());

        // when
        sut.dispatchOnChatMessageSent(ROOM_ID, 100L, SENDER_ID, "안녕하세요");

        // then
        ArgumentCaptor<List<Long>> userIdsCaptor = ArgumentCaptor.captor();
        then(userOnlineStatusFetchPort).should().getUserFocusRooms(userIdsCaptor.capture());
        assertThat(userIdsCaptor.getValue()).containsExactly(2L, 3L);

        then(userOnlineStatusFetchPort).should(never()).getUserFocusRoom(any());
    }

    @Test
    @DisplayName("해당 방을 보고 있는 수신자에게는 실시간 알림을 보내지 않는다")
    void 포커스_중인_수신자에게는_전송하지_않는다() {
        // given
        given(chatRoomFetchPort.getChatRoomRecipients(ROOM_ID, SENDER_ID)).willReturn(List.of(
                new NotificationChatRecipientInfo(2L, true),
                new NotificationChatRecipientInfo(3L, true)
        ));
        given(userFetchPort.getUser(SENDER_ID)).willReturn(user(SENDER_ID, "발신자", true));
        given(userFetchPort.getUsers(anyList())).willReturn(List.of(
                user(2L, "수신자2", true),
                user(3L, "수신자3", true)
        ));
        // 2번은 이 방을 보고 있고, 3번은 다른 방을 보고 있다
        given(userOnlineStatusFetchPort.getUserFocusRooms(anyList()))
                .willReturn(Map.of(2L, ROOM_ID, 3L, 99L));

        // when
        sut.dispatchOnChatMessageSent(ROOM_ID, 100L, SENDER_ID, "안녕하세요");

        // then - 방 인원 전체가 한 건으로 묶여 나가되, 포커스 중인 2번은 목록에서 빠진다
        ArgumentCaptor<List<Long>> dispatchedIds = ArgumentCaptor.captor();
        then(notificationDispatchUseCase).should().dispatchAll(dispatchedIds.capture(), any());
        assertThat(dispatchedIds.getValue()).containsExactly(3L);

        then(outboxDispatchUseCase).should(never()).sendPendingOutboxImmediately(2L);
        then(outboxDispatchUseCase).should().sendPendingOutboxImmediately(3L);
    }

    @Test
    @DisplayName("알림이 꺼진 수신자에게도 STOMP 메시지는 보내되 아웃박스 발송은 하지 않는다")
    void 알림이_꺼진_수신자도_stomp_는_받는다() {
        // given: 2번은 방 알림 OFF, 3번은 글로벌 채팅 알림 OFF
        given(chatRoomFetchPort.getChatRoomRecipients(ROOM_ID, SENDER_ID)).willReturn(List.of(
                new NotificationChatRecipientInfo(2L, false),
                new NotificationChatRecipientInfo(3L, true)
        ));
        given(userFetchPort.getUser(SENDER_ID)).willReturn(user(SENDER_ID, "발신자", true));
        given(userFetchPort.getUsers(anyList())).willReturn(List.of(
                user(2L, "수신자2", true),
                user(3L, "수신자3", false)
        ));
        given(userOnlineStatusFetchPort.getUserFocusRooms(anyList())).willReturn(Map.of());

        // when
        sut.dispatchOnChatMessageSent(ROOM_ID, 100L, SENDER_ID, "안녕하세요");

        // then - 알림 설정과 무관하게 둘 다 STOMP 대상에 포함된다
        ArgumentCaptor<List<Long>> dispatchedIds = ArgumentCaptor.captor();
        then(notificationDispatchUseCase).should().dispatchAll(dispatchedIds.capture(), any());
        assertThat(dispatchedIds.getValue()).containsExactly(2L, 3L);

        then(outboxDispatchUseCase).should(never()).sendPendingOutboxImmediately(any());
    }

    private static NotificationUserInfo user(Long id, String nickName, boolean chatAlarmEnabled) {
        return new NotificationUserInfo(id, nickName, null, "token-" + id, chatAlarmEnabled, true, true);
    }
}
