package com.back.catchmate.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.chat.entity.ChatRoom;
import com.back.catchmate.chat.repository.ChatRoomRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatQueryApiTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private ChatQueryApi chatQueryApi;

    @Test
    @DisplayName("게시글의 채팅방 ID 를 돌려주고, 없으면 empty")
    void findChatRoomIdByBoardId() {
        // given
        ChatRoom room = ChatRoom.createChatRoom(10L);
        ReflectionTestUtils.setField(room, "id", 7L);
        given(chatRoomRepository.findByBoardId(10L)).willReturn(Optional.of(room));
        given(chatRoomRepository.findByBoardId(11L)).willReturn(Optional.empty());

        // when & then
        assertThat(chatQueryApi.findChatRoomIdByBoardId(10L)).contains(7L);
        assertThat(chatQueryApi.findChatRoomIdByBoardId(11L)).isEmpty();
    }
}
