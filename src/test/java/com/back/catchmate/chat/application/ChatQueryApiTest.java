package com.back.catchmate.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.chat.application.dto.api.ChatRecipientInfo;
import com.back.catchmate.chat.domain.ChatFocusRoomStore;
import com.back.catchmate.chat.domain.ChatRoom;
import com.back.catchmate.chat.domain.ChatRoomMember;
import com.back.catchmate.chat.domain.ChatRoomMemberRepository;
import com.back.catchmate.chat.domain.ChatRoomRepository;
import com.back.catchmate.chat.fixture.ChatFixture;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatQueryApiTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatFocusRoomStore chatFocusRoomStore;

    @InjectMocks
    private ChatQueryApi chatQueryApi;

    @Test
    @DisplayName("게시글의 채팅방 ID 를 돌려주고, 없으면 empty")
    void findChatRoomIdByBoardId() {
        given(chatRoomRepository.findByBoardId(10L)).willReturn(Optional.of(ChatFixture.room(7L, 10L, 0)));
        given(chatRoomRepository.findByBoardId(11L)).willReturn(Optional.empty());

        assertThat(chatQueryApi.findChatRoomIdByBoardId(10L)).contains(7L);
        assertThat(chatQueryApi.findChatRoomIdByBoardId(11L)).isEmpty();
    }

    @Test
    @DisplayName("알림 수신자는 발신자를 뺀 활성 멤버와 그 방의 알림 설정이다")
    void getRecipients() {
        // given
        ChatRoom room = ChatFixture.room(5L, 10L, 0);
        ChatRoomMember sender = ChatFixture.member(1L, room, 1L, 0);
        ChatRoomMember muted = ChatFixture.member(2L, room, 2L, 0);
        muted.disableNotification();
        given(chatRoomMemberRepository.findActiveByChatRoomId(5L)).willReturn(List.of(sender, muted));

        // when & then
        assertThat(chatQueryApi.getRecipients(5L, 1L)).containsExactly(new ChatRecipientInfo(2L, false));
    }

    @Test
    @DisplayName("포커스 방 조회는 포커스 방 저장소에 그대로 위임한다")
    void delegatesFocusRooms() {
        given(chatFocusRoomStore.find(1L)).willReturn(Optional.of(11L));
        given(chatFocusRoomStore.findAll(List.of(1L, 2L))).willReturn(Map.of(1L, 11L));

        assertThat(chatQueryApi.findFocusRoom(1L)).contains(11L);
        assertThat(chatQueryApi.getFocusRooms(List.of(1L, 2L))).containsOnly(Map.entry(1L, 11L));
    }
}
