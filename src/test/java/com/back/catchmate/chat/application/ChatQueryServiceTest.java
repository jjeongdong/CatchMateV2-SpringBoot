package com.back.catchmate.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.chat.application.dto.result.ChatMessageResult;
import com.back.catchmate.chat.application.dto.result.ChatRoomResult;
import com.back.catchmate.chat.domain.ChatHistoryPage;
import com.back.catchmate.chat.domain.ChatMessageRepository;
import com.back.catchmate.chat.domain.ChatRoom;
import com.back.catchmate.chat.domain.ChatRoomMember;
import com.back.catchmate.chat.domain.ChatRoomMemberRepository;
import com.back.catchmate.chat.domain.ChatRoomRepository;
import com.back.catchmate.chat.domain.MembershipSnapshot;
import com.back.catchmate.chat.domain.MessageType;
import com.back.catchmate.chat.domain.exception.ChatCursorInvalidException;
import com.back.catchmate.chat.domain.exception.ChatRoomMemberNotFoundException;
import com.back.catchmate.chat.domain.exception.ChatSubscriptionDestinationInvalidException;
import com.back.catchmate.chat.fixture.ChatFixture;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.global.response.CursorPageResult;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.UserQueryApi;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatQueryServiceTest {

    private static final Long ROOM_ID = 5L;
    private static final Long USER_ID = 1L;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatMembershipReader chatMembershipReader;

    @Mock
    private ChatReadRecorder chatReadRecorder;

    @Mock
    private ChatHistoryReader chatHistoryReader;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private BoardQueryApi boardQueryApi;

    @Mock
    private GameQueryApi gameQueryApi;

    @Mock
    private ClubQueryApi clubQueryApi;

    @InjectMocks
    private ChatQueryService chatQueryService;

    private static ChatHistoryPage.Entry entry(long id) {
        return new ChatHistoryPage.Entry(id, ROOM_ID, USER_ID, "철수", null, "m" + id, MessageType.TEXT, ChatFixture.NOW);
    }

    @Test
    @DisplayName("내 채팅방 목록은 안 읽은 수·알림 설정·마지막 메시지·게시글을 BC 마다 한 번씩 모아 조립한다")
    void getMyChatRooms() {
        // given
        ChatRoom room = ChatFixture.room(ROOM_ID, 10L, 7);
        ChatRoomMember me = ChatFixture.member(1L, room, USER_ID, 3);
        given(chatRoomRepository.findAllByMemberUserId(USER_ID, 0L, 20)).willReturn(List.of(room));
        given(chatRoomRepository.countByMemberUserId(USER_ID)).willReturn(1L);
        given(chatMessageRepository.findLastTextByChatRoomIds(List.of(ROOM_ID)))
                .willReturn(Map.of(ROOM_ID, ChatFixture.text(100L, room, 2L, "안녕")));
        given(chatRoomMemberRepository.findActiveByChatRoomIdsAndUserId(List.of(ROOM_ID), USER_ID))
                .willReturn(List.of(me));
        given(boardQueryApi.getInfos(List.of(10L)))
                .willReturn(Map.of(
                        10L, new BoardInfo(10L, "직관", "c", 4, 2, 9L, null, null, null, List.of(), true, null, null)));
        given(userQueryApi.getInfos(any())).willReturn(Map.of());

        // when
        OffsetPageResult<ChatRoomResult> result = chatQueryService.getMyChatRooms(USER_ID, 0, 20);

        // then
        ChatRoomResult item = result.content().get(0);
        assertThat(item.unreadCount()).isEqualTo(4L);
        assertThat(item.isNotificationOn()).isTrue();
        assertThat(item.lastMessage().content()).isEqualTo("안녕");
        assertThat(item.board().title()).isEqualTo("직관");
        then(gameQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지난 메시지는 size+1 개를 읽어 더 있으면 가장 오래된 것을 빼고 그 다음 것을 커서로 준다")
    void getMessagesCursor() {
        // given
        given(chatMembershipReader.get(ROOM_ID, USER_ID)).willReturn(new MembershipSnapshot(true, false));
        given(chatHistoryReader.read(ROOM_ID, null, 3))
                .willReturn(new ChatHistoryPage(List.of(entry(1), entry(2), entry(3))));

        // when
        CursorPageResult<ChatMessageResult> result = chatQueryService.getMessages(USER_ID, ROOM_ID, null, 2);

        // then
        assertThat(result.content()).extracting(ChatMessageResult::messageId).containsExactly(2L, 3L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("2");
        then(chatReadRecorder).should().record(ROOM_ID, USER_ID);
    }

    @Test
    @DisplayName("숫자가 아닌 커서는 ChatCursorInvalidException")
    void rejectsInvalidCursor() {
        given(chatMembershipReader.get(ROOM_ID, USER_ID)).willReturn(new MembershipSnapshot(true, false));

        assertThatThrownBy(() -> chatQueryService.getMessages(USER_ID, ROOM_ID, "abc", 20))
                .isInstanceOf(ChatCursorInvalidException.class);
    }

    @Test
    @DisplayName("동기화는 커서 이후 size+1 개를 읽어 초과분이 있으면 hasNext 다")
    void syncMessages() {
        // given
        ChatRoom room = ChatFixture.room(ROOM_ID, 10L, 0);
        given(chatMembershipReader.get(ROOM_ID, USER_ID)).willReturn(new MembershipSnapshot(true, false));
        given(chatMessageRepository.findAfter(ROOM_ID, 10L, 3))
                .willReturn(List.of(
                        ChatFixture.text(11L, room, 2L, "a"),
                        ChatFixture.text(12L, room, 2L, "b"),
                        ChatFixture.text(13L, room, 2L, "c")));
        given(userQueryApi.getInfos(List.of(2L))).willReturn(Map.of());

        // when
        CursorPageResult<ChatMessageResult> result = chatQueryService.syncMessages(USER_ID, ROOM_ID, 10L, 2);

        // then
        assertThat(result.content()).extracting(ChatMessageResult::messageId).containsExactly(11L, 12L);
        assertThat(result.nextCursor()).isEqualTo("12");
    }

    @Test
    @DisplayName("비멤버는 마지막 메시지를 볼 수 없다")
    void lastMessageRequiresMembership() {
        willThrow(new ChatRoomMemberNotFoundException())
                .given(chatMembershipReader)
                .get(ROOM_ID, USER_ID);

        assertThatThrownBy(() -> chatQueryService.getLastMessage(USER_ID, ROOM_ID))
                .isInstanceOf(ChatRoomMemberNotFoundException.class);
        then(chatMessageRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("구독 주소의 방 번호가 숫자가 아니면 멤버 확인 없이 거절한다")
    void rejectsInvalidSubscription() {
        for (String chatRoomId : new String[] {"*", "", "abc", "5/x"}) {
            assertThatThrownBy(() -> chatQueryService.verifySubscription(USER_ID, chatRoomId))
                    .isInstanceOf(ChatSubscriptionDestinationInvalidException.class);
        }
        then(chatMembershipReader).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("구독은 활성 멤버만 허용한다")
    void subscriptionRequiresMembership() {
        given(chatMembershipReader.get(ROOM_ID, USER_ID)).willReturn(new MembershipSnapshot(false, false));

        assertThatThrownBy(() -> chatQueryService.verifySubscription(USER_ID, "5"))
                .isInstanceOf(ChatRoomMemberNotFoundException.class);
    }

    @Test
    @DisplayName("비멤버는 참여자 목록을 볼 수 없다")
    void membersRequireMembership() {
        given(chatMembershipReader.get(ROOM_ID, USER_ID)).willReturn(new MembershipSnapshot(false, false));

        assertThatThrownBy(() -> chatQueryService.getChatRoomMembers(USER_ID, ROOM_ID))
                .isInstanceOf(ChatRoomMemberNotFoundException.class);
        then(chatRoomMemberRepository).shouldHaveNoInteractions();
    }
}
