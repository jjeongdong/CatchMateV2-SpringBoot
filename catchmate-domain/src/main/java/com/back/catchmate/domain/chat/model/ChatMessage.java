package com.back.catchmate.domain.chat.model;

import com.back.catchmate.chat.enums.MessageType;
import com.back.catchmate.domain.user.model.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {
    private Long id;
    private ChatRoom chatRoom;
    private User sender;
    private String content;
    private Long sequence;
    private MessageType messageType;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    // 채팅 메시지 생성 메서드
    public static ChatMessage createMessage(ChatRoom chatRoom, User sender, String content, MessageType messageType) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(content)
                .sequence(chatRoom.getLastMessageSequence())
                .messageType(messageType)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 삭제 메서드
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    // 삭제 여부 확인
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
