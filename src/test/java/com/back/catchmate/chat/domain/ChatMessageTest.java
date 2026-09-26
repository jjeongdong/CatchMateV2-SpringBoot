package com.back.catchmate.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.chat.domain.exception.ChatMessageTypeNotAllowedException;
import com.back.catchmate.chat.fixture.ChatFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatMessageTest {

    private final ChatRoom room = ChatFixture.room(5L, 10L, 0);

    @Test
    @DisplayName("입장·퇴장·강퇴 안내는 시스템 메시지이고 문구는 옛 것과 같다")
    void systemMessages() {
        assertThat(ChatMessage.joined(room, 1L, "철수", 3L).getContent()).isEqualTo("철수님이 입장하셨습니다.");
        assertThat(ChatMessage.left(room, 1L, "철수", 3L).getContent()).isEqualTo("철수님이 퇴장하셨습니다.");
        assertThat(ChatMessage.kicked(room, 1L, "철수", 3L).getContent()).isEqualTo("철수님이 내보내졌습니다.");
        assertThat(ChatMessage.joined(room, 1L, "철수", 3L).getMessageType()).isEqualTo(MessageType.SYSTEM);
    }

    @Test
    @DisplayName("사용자 메시지는 TEXT 이고 순번을 가진다")
    void text() {
        ChatMessage message = ChatMessage.text(room, 1L, "안녕", 7L);

        assertThat(message.getMessageType()).isEqualTo(MessageType.TEXT);
        assertThat(message.getSequence()).isEqualTo(7L);
        assertThat(message.getChatRoom()).isSameAs(room);
    }

    @Test
    @DisplayName("사용자는 SYSTEM 타입을 보낼 수 없다 (시스템 메시지 사칭 차단)")
    void userCannotSendSystem() {
        assertThatCode(MessageType.TEXT::verifySendableByUser).doesNotThrowAnyException();
        assertThatThrownBy(MessageType.SYSTEM::verifySendableByUser)
                .isInstanceOf(ChatMessageTypeNotAllowedException.class);
    }
}
