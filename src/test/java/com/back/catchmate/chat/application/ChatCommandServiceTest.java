package com.back.catchmate.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.chat.application.dto.command.ChatMessageSendCommand;
import com.back.catchmate.chat.domain.ChatFocusRoomStore;
import com.back.catchmate.chat.domain.ChatMembershipCache;
import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.ChatMessageRepository;
import com.back.catchmate.chat.domain.ChatRoom;
import com.back.catchmate.chat.domain.ChatRoomImageUploader;
import com.back.catchmate.chat.domain.ChatRoomMember;
import com.back.catchmate.chat.domain.ChatRoomMemberRepository;
import com.back.catchmate.chat.domain.ChatRoomRepository;
import com.back.catchmate.chat.domain.ChatRoomSequenceBuffer;
import com.back.catchmate.chat.domain.ChatSequenceStore;
import com.back.catchmate.chat.domain.MembershipSnapshot;
import com.back.catchmate.chat.domain.MessageType;
import com.back.catchmate.chat.domain.ReadSequence;
import com.back.catchmate.chat.domain.ReadSequenceBuffer;
import com.back.catchmate.chat.domain.event.ChatMessageBroadcastEvent;
import com.back.catchmate.chat.domain.exception.ChatMessageTypeNotAllowedException;
import com.back.catchmate.chat.domain.exception.ChatRoomMemberNotFoundException;
import com.back.catchmate.chat.domain.exception.ChatRoomNotHostException;
import com.back.catchmate.chat.domain.exception.ChatRoomReadOnlyException;
import com.back.catchmate.chat.domain.exception.ChatRoomReentryNotAllowedException;
import com.back.catchmate.chat.domain.exception.ChatSenderUnauthenticatedException;
import com.back.catchmate.chat.fixture.ChatFixture;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatCommandServiceTest {

    private static final Long ROOM_ID = 5L;
    private static final Long USER_ID = 1L;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatSequenceStore chatSequenceStore;

    @Mock
    private ChatRoomSequenceBuffer chatRoomSequenceBuffer;

    @Mock
    private ReadSequenceBuffer readSequenceBuffer;

    @Mock
    private ChatMembershipCache chatMembershipCache;

    @Mock
    private ChatFocusRoomStore chatFocusRoomStore;

    @Mock
    private ChatRoomImageUploader chatRoomImageUploader;

    @Mock
    private ChatMembershipReader chatMembershipReader;

    @Mock
    private ChatReadRecorder chatReadRecorder;

    @Mock
    private ChatMessageWriter chatMessageWriter;

    @Mock
    private ChatBufferFlushExecutor chatBufferFlushExecutor;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private BoardQueryApi boardQueryApi;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatCommandService chatCommandService;

    static UserInfo user(Long userId, String nickName) {
        return new UserInfo(
                userId,
                "u@catchmate.com",
                null,
                null,
                'M',
                nickName,
                LocalDate.of(2000, 1, 1),
                "응원형",
                "img-" + userId,
                "ROLE_USER",
                null,
                null,
                false,
                false,
                false,
                false,
                null,
                null);
    }

    private void saveReturnsWithId() {
        willAnswer(invocation -> {
                    ChatMessage message = invocation.getArgument(0);
                    ReflectionTestUtils.setField(message, "id", 100L);
                    return message;
                })
                .given(chatMessageRepository)
                .save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("멤버십 확인이 시퀀스 발급보다 먼저고, 저장 뒤 버퍼링·기록 캐시를 비운다")
    void sendMessageOrder() {
        // given
        given(userQueryApi.getInfo(USER_ID)).willReturn(user(USER_ID, "철수"));
        given(chatMembershipReader.get(ROOM_ID, USER_ID)).willReturn(new MembershipSnapshot(true, false));
        given(chatSequenceStore.next(ROOM_ID)).willReturn(42L);

        // when
        chatCommandService.sendMessage(USER_ID, new ChatMessageSendCommand(ROOM_ID, "안녕", MessageType.TEXT));

        // then
        InOrder order = inOrder(chatMembershipReader, chatSequenceStore, chatMessageWriter, chatRoomSequenceBuffer);
        order.verify(chatMembershipReader).get(ROOM_ID, USER_ID);
        order.verify(chatSequenceStore).next(ROOM_ID);
        order.verify(chatMessageWriter).writeText(ROOM_ID, user(USER_ID, "철수"), "안녕", 42L);
        order.verify(chatRoomSequenceBuffer).buffer(ROOM_ID, 42L);
        then(readSequenceBuffer).should().buffer(ROOM_ID, USER_ID, 42L);
    }

    @Test
    @DisplayName("비멤버·읽기 전용이면 시퀀스를 발급하지 않고 거절한다")
    void rejectsNonMemberBeforeSequence() {
        // given
        given(userQueryApi.getInfo(USER_ID)).willReturn(user(USER_ID, "철수"));
        given(chatMembershipReader.get(ROOM_ID, USER_ID))
                .willReturn(new MembershipSnapshot(false, false))
                .willReturn(new MembershipSnapshot(true, true));
        ChatMessageSendCommand command = new ChatMessageSendCommand(ROOM_ID, "안녕", MessageType.TEXT);

        // when & then
        assertThatThrownBy(() -> chatCommandService.sendMessage(USER_ID, command))
                .isInstanceOf(ChatRoomMemberNotFoundException.class);
        assertThatThrownBy(() -> chatCommandService.sendMessage(USER_ID, command))
                .isInstanceOf(ChatRoomReadOnlyException.class);
        then(chatSequenceStore).shouldHaveNoInteractions();
        then(chatMessageWriter).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("STOMP Principal 이 비어 발신자를 모르면 어떤 조회·발급도 하지 않고 거절한다")
    void rejectsUnauthenticatedSender() {
        ChatMessageSendCommand command = new ChatMessageSendCommand(ROOM_ID, "안녕", MessageType.TEXT);

        assertThatThrownBy(() -> chatCommandService.sendMessage(null, command))
                .isInstanceOf(ChatSenderUnauthenticatedException.class);
        assertThatThrownBy(() -> chatCommandService.leaveChatRoom(null, ROOM_ID))
                .isInstanceOf(ChatSenderUnauthenticatedException.class);
        then(chatMembershipReader).shouldHaveNoInteractions();
        then(chatRoomMemberRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SYSTEM 타입은 어떤 조회·발급보다 먼저 거절한다 (시스템 메시지 사칭 차단)")
    void rejectsSystemMessageFirst() {
        ChatMessageSendCommand command = new ChatMessageSendCommand(ROOM_ID, "○○님이 입장하셨습니다.", MessageType.SYSTEM);

        assertThatThrownBy(() -> chatCommandService.sendMessage(USER_ID, command))
                .isInstanceOf(ChatMessageTypeNotAllowedException.class);
        then(userQueryApi).shouldHaveNoInteractions();
        then(chatMembershipReader).shouldHaveNoInteractions();
        then(chatSequenceStore).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("저장 뒤 버퍼링이 실패해도 예외를 올리지 않는다 (재전송으로 중복 저장되지 않게)")
    void swallowsBufferFailure() {
        // given
        given(userQueryApi.getInfo(USER_ID)).willReturn(user(USER_ID, "철수"));
        given(chatMembershipReader.get(ROOM_ID, USER_ID)).willReturn(new MembershipSnapshot(true, false));
        given(chatSequenceStore.next(ROOM_ID)).willReturn(42L);
        willThrow(new QueryTimeoutException("redis"))
                .given(chatRoomSequenceBuffer)
                .buffer(ROOM_ID, 42L);

        // when & then
        assertThatCode(() -> chatCommandService.sendMessage(
                        USER_ID, new ChatMessageSendCommand(ROOM_ID, "안녕", MessageType.TEXT)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("퇴장하면 멤버를 비활성화하고 캐시를 비운 뒤 퇴장 메시지를 방송한다")
    void leaveChatRoom() {
        // given
        ChatRoom room = ChatFixture.room(ROOM_ID, 10L, 3);
        ChatRoomMember member = ChatFixture.member(1L, room, USER_ID, 3);
        given(userQueryApi.getInfo(USER_ID)).willReturn(user(USER_ID, "철수"));
        given(chatSequenceStore.current(ROOM_ID)).willReturn(3L);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(ROOM_ID, USER_ID))
                .willReturn(Optional.of(member));
        given(chatRoomRepository.getReference(ROOM_ID)).willReturn(room);
        saveReturnsWithId();

        // when
        chatCommandService.leaveChatRoom(USER_ID, ROOM_ID);

        // then
        assertThat(member.isActive()).isFalse();
        then(chatMembershipCache).should().evict(ROOM_ID, USER_ID);
        ArgumentCaptor<ChatMessageBroadcastEvent> event = ArgumentCaptor.forClass(ChatMessageBroadcastEvent.class);
        then(eventPublisher).should().publishEvent(event.capture());
        assertThat(event.getValue().content()).isEqualTo("철수님이 퇴장하셨습니다.");
    }

    @Test
    @DisplayName("방장이 아니면 강퇴하지 못하고 대상 멤버를 건드리지 않는다")
    void kickRequiresHost() {
        // given
        given(chatRoomRepository.getById(ROOM_ID)).willReturn(ChatFixture.room(ROOM_ID, 10L, 0));
        given(chatSequenceStore.current(ROOM_ID)).willReturn(0L);
        given(boardQueryApi.getInfo(10L)).willReturn(board(10L, 9L));

        // when & then
        assertThatThrownBy(() -> chatCommandService.kickChatRoomMember(USER_ID, ROOM_ID, 2L))
                .isInstanceOf(ChatRoomNotHostException.class);
        then(chatRoomMemberRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("새 멤버는 참여 시점의 방 시퀀스로 추가되고 입장 메시지를 남긴다")
    void addNewMember() {
        // given
        ChatRoom room = ChatFixture.room(ROOM_ID, 10L, 7);
        given(chatRoomRepository.findByBoardId(10L)).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(ROOM_ID, USER_ID))
                .willReturn(Optional.empty());
        given(userQueryApi.getInfo(USER_ID)).willReturn(user(USER_ID, "철수"));
        given(chatSequenceStore.current(ROOM_ID)).willReturn(7L);
        saveReturnsWithId();

        // when
        chatCommandService.addBoardChatRoomMember(10L, USER_ID);

        // then
        ArgumentCaptor<ChatRoomMember> member = ArgumentCaptor.forClass(ChatRoomMember.class);
        then(chatRoomMemberRepository).should().save(member.capture());
        assertThat(member.getValue().getLastReadSequence()).isEqualTo(7L);
        then(chatMembershipCache).should().evict(ROOM_ID, USER_ID);
        then(eventPublisher).should().publishEvent(any(ChatMessageBroadcastEvent.class));
    }

    @Test
    @DisplayName("방이 없으면 만들고, 스스로 나간 멤버의 재입장은 거절해 원래 작업(수락)도 취소되게 한다")
    void addMemberCreatesRoomAndRejectsReentry() {
        // given
        ChatRoom room = ChatFixture.room(ROOM_ID, 10L, 0);
        ChatRoomMember left = ChatFixture.member(1L, room, USER_ID, 0);
        left.leave(ChatFixture.NOW);
        given(chatRoomRepository.findByBoardId(10L)).willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(room);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(ROOM_ID, USER_ID))
                .willReturn(Optional.of(left));

        // when & then
        assertThatThrownBy(() -> chatCommandService.addBoardChatRoomMember(10L, USER_ID))
                .isInstanceOf(ChatRoomReentryNotAllowedException.class);
        then(chatRoomRepository).should().save(any(ChatRoom.class));
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("버퍼 반영이 실패하면 꺼낸 값을 버퍼로 되돌린다")
    void flushRestoresOnFailure() {
        // given
        given(readSequenceBuffer.drainAll()).willReturn(List.of(new ReadSequence(ROOM_ID, USER_ID, 9L)));
        willThrow(new QueryTimeoutException("db"))
                .given(chatBufferFlushExecutor)
                .flushReadSequences(any());
        given(chatRoomSequenceBuffer.drainAll()).willReturn(Map.of(ROOM_ID, 9L));
        willThrow(new QueryTimeoutException("db"))
                .given(chatBufferFlushExecutor)
                .flushRoomSequences(any());

        // when
        chatCommandService.flushReadSequences();
        chatCommandService.flushRoomSequences();

        // then
        then(readSequenceBuffer).should().buffer(ROOM_ID, USER_ID, 9L);
        then(chatRoomSequenceBuffer).should().buffer(ROOM_ID, 9L);
    }

    @Test
    @DisplayName("버퍼가 비어 있으면 DB 에 가지 않는다")
    void flushSkipsEmpty() {
        given(readSequenceBuffer.drainAll()).willReturn(List.of());

        chatCommandService.flushReadSequences();

        then(chatBufferFlushExecutor).should(never()).flushReadSequences(any());
    }

    private static BoardInfo board(Long boardId, Long writerId) {
        return new BoardInfo(boardId, "t", "c", 4, 1, writerId, 1L, 100L, null, List.of(), true, null, null);
    }
}
