package com.back.catchmate.chat.fixture;

import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.ChatRoom;
import com.back.catchmate.chat.domain.ChatRoomMember;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public final class ChatFixture {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 1, 12, 0);

    private ChatFixture() {}

    // 저장 없이 쓰는 단위 테스트용이라 id·시각을 리플렉션으로 채운다.
    public static ChatRoom room(Long chatRoomId, Long boardId, long lastMessageSequence) {
        ChatRoom room = ChatRoom.create(boardId);
        ReflectionTestUtils.setField(room, "id", chatRoomId);
        ReflectionTestUtils.setField(room, "lastMessageSequence", lastMessageSequence);
        ReflectionTestUtils.setField(room, "createdAt", NOW);
        return room;
    }

    public static ChatRoomMember member(Long memberId, ChatRoom room, Long userId, long lastReadSequence) {
        ChatRoomMember member = ChatRoomMember.create(room, userId, lastReadSequence, NOW);
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    public static ChatMessage text(Long messageId, ChatRoom room, Long senderId, String content) {
        ChatMessage message = ChatMessage.text(room, senderId, content, messageId);
        ReflectionTestUtils.setField(message, "id", messageId);
        ReflectionTestUtils.setField(message, "createdAt", NOW);
        return message;
    }
}
