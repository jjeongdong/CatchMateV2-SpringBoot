package com.back.catchmate.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.chat.domain.exception.ChatRoomNotHostException;
import com.back.catchmate.chat.domain.exception.ChatSelfKickNotAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatRoomTest {

    @Test
    @DisplayName("새 채팅방은 시퀀스 0 에서 시작한다")
    void create() {
        assertThat(ChatRoom.create(10L).getLastMessageSequence()).isZero();
    }

    @Test
    @DisplayName("방장(게시글 작성자)만 강퇴할 수 있고, 자기 자신은 강퇴할 수 없다")
    void verifyKick() {
        ChatRoom room = ChatRoom.create(10L);

        assertThatCode(() -> room.verifyKick(1L, 1L, 2L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> room.verifyKick(2L, 1L, 3L)).isInstanceOf(ChatRoomNotHostException.class);
        assertThatThrownBy(() -> room.verifyKick(1L, 1L, 1L)).isInstanceOf(ChatSelfKickNotAllowedException.class);
    }

    @Test
    @DisplayName("방장이 아니면 자기 강퇴 여부보다 먼저 거절한다 (옛 확인 순서)")
    void hostCheckComesFirst() {
        assertThatThrownBy(() -> ChatRoom.create(10L).verifyKick(2L, 1L, 2L))
                .isInstanceOf(ChatRoomNotHostException.class);
    }
}
