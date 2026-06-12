package com.back.catchmate.chat.domain.model;

import com.back.catchmate.chat.domain.enums.MessageType;
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
    private Long chatRoomId;
    private Long senderId;
    private String content;
    private Long sequence;
    private MessageType messageType;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    // 채팅 메시지 생성 메서드
    public static ChatMessage createMessage(Long chatRoomId, Long senderId, String content, MessageType messageType, Long sequence) {
        return ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .content(content)
                .sequence(sequence)
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
