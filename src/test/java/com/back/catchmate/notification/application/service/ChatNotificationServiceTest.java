package com.back.catchmate.notification.application.service;

import com.back.catchmate.notification.application.port.in.OutboxRecipient;
import com.back.catchmate.notification.application.port.in.OutboxSaveUseCase;
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
class ChatNotificationServiceTest {

    private static final Long ROOM_ID = 7L;
    private static final Long SENDER_ID = 1L;

    @Mock
    private UserFetchPort userFetchPort;

    @Mock
    private ChatRoomFetchPort chatRoomFetchPort;

    @Mock
    private UserOnlineStatusFetchPort userOnlineStatusFetchPort;

    @Mock
    private OutboxSaveUseCase outboxSaveUseCase;

    @InjectMocks
    private ChatNotificationService sut;

    @Test
    @DisplayName("알림 설정으로 걸러진 수신자는 포커스 방 조회 대상에서 제외하고, 조회는 한 번만 한다")
    void 필터를_통과한_수신자만_한_번의_배치로_조회한다() {
        // given: 통과 2명(2·6) / 방 알림 OFF(3) / 채팅 알림 OFF(4) / 토큰 없음(5)
        given(chatRoomFetchPort.getChatRoomRecipients(ROOM_ID, SENDER_ID)).willReturn(List.of(
                new NotificationChatRecipientInfo(2L, true),
                new NotificationChatRecipientInfo(3L, false),
                new NotificationChatRecipientInfo(4L, true),
                new NotificationChatRecipientInfo(5L, true),
                new NotificationChatRecipientInfo(6L, true)
        ));
        given(userFetchPort.getUser(SENDER_ID)).willReturn(user(SENDER_ID, "발신자", "token-1"));
        given(userFetchPort.getUsers(anyList())).willReturn(List.of(
                user(2L, "수신자2", "token-2"),
                user(3L, "수신자3", "token-3"),
                userWithChatAlarmOff(4L, "수신자4", "token-4"),
                user(5L, "수신자5", null),
                user(6L, "수신자6", "token-6")
        ));
        given(userOnlineStatusFetchPort.getUserFocusRooms(anyList())).willReturn(Map.of());

        // when
        sut.saveOnChatMessageSent(ROOM_ID, 100L, SENDER_ID, "안녕하세요");

        // then: 배치 조회 1회, 인자는 필터 통과자만
        ArgumentCaptor<List<Long>> userIdsCaptor = ArgumentCaptor.captor();
        then(userOnlineStatusFetchPort).should().getUserFocusRooms(userIdsCaptor.capture());
        assertThat(userIdsCaptor.getValue()).containsExactly(2L, 6L);

        // 단건 조회로 되돌아가지 않았는지 확인
        then(userOnlineStatusFetchPort).should(never()).getUserFocusRoom(any());
    }

    @Test
    @DisplayName("해당 방을 보고 있는 수신자는 아웃박스 저장 대상에서 제외한다")
    void 포커스_중인_수신자는_아웃박스에_저장하지_않는다() {
        // given
        given(chatRoomFetchPort.getChatRoomRecipients(ROOM_ID, SENDER_ID)).willReturn(List.of(
                new NotificationChatRecipientInfo(2L, true),
                new NotificationChatRecipientInfo(3L, true)
        ));
        given(userFetchPort.getUser(SENDER_ID)).willReturn(user(SENDER_ID, "발신자", "token-1"));
        given(userFetchPort.getUsers(anyList())).willReturn(List.of(
                user(2L, "수신자2", "token-2"),
                user(3L, "수신자3", "token-3")
        ));
        // 2번은 이 방을 보고 있고, 3번은 다른 방을 보고 있다
        given(userOnlineStatusFetchPort.getUserFocusRooms(anyList()))
                .willReturn(Map.of(2L, ROOM_ID, 3L, 99L));

        // when
        sut.saveOnChatMessageSent(ROOM_ID, 100L, SENDER_ID, "안녕하세요");

        // then
        ArgumentCaptor<List<OutboxRecipient>> recipientsCaptor = ArgumentCaptor.captor();
        then(outboxSaveUseCase).should().saveOutboxBatch(recipientsCaptor.capture(), any(), any(), any());
        assertThat(recipientsCaptor.getValue())
                .containsExactly(new OutboxRecipient(3L, "token-3"));
    }

    @Test
    @DisplayName("필터를 통과한 수신자가 없어도 빈 목록으로 아웃박스 저장을 호출한다")
    void 통과한_수신자가_없으면_빈_목록으로_저장을_호출한다() {
        // given: 전원 방 알림 OFF
        given(chatRoomFetchPort.getChatRoomRecipients(ROOM_ID, SENDER_ID)).willReturn(List.of(
                new NotificationChatRecipientInfo(2L, false)
        ));
        given(userFetchPort.getUser(SENDER_ID)).willReturn(user(SENDER_ID, "발신자", "token-1"));
        given(userFetchPort.getUsers(anyList())).willReturn(List.of(user(2L, "수신자2", "token-2")));
        given(userOnlineStatusFetchPort.getUserFocusRooms(List.of())).willReturn(Map.of());

        // when
        sut.saveOnChatMessageSent(ROOM_ID, 100L, SENDER_ID, "안녕하세요");

        // then
        then(outboxSaveUseCase).should().saveOutboxBatch(List.of(), "발신자", "안녕하세요", Map.of(
                "type", "CHAT",
                "roomId", "7",
                "senderId", "1",
                "senderNickname", "발신자",
                "content", "안녕하세요",
                "title", "발신자",
                "body", "안녕하세요"
        ));
    }

    private static NotificationUserInfo user(Long id, String nickName, String fcmToken) {
        return new NotificationUserInfo(id, nickName, null, fcmToken, true, true, true);
    }

    private static NotificationUserInfo userWithChatAlarmOff(Long id, String nickName, String fcmToken) {
        return new NotificationUserInfo(id, nickName, null, fcmToken, false, true, true);
    }
}
