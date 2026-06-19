package com.back.catchmate.chat.domain.model;

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
public class ChatRoom {
    private Long id;
    private Long boardId;
    private Long lastMessageSequence;
    private String chatRoomImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    // 채팅방 생성 메서드
    public static ChatRoom createChatRoom(Long boardId) {
        return ChatRoom.builder()
                .boardId(boardId)
                .lastMessageSequence(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 채팅 메시지 시퀀스 증가 메서드
    public void updateLastMessageSequence(Long sequence) {
        // 혹시 모를 과거 시퀀스 덮어쓰기 방지
        if (this.lastMessageSequence == null || this.lastMessageSequence < sequence) {
            this.lastMessageSequence = sequence;
        }
    }

    // 채팅방 이미지 URL 업데이트 메서드
    public void updateImageUrl(String chatRoomImageUrl) {
        this.chatRoomImageUrl = chatRoomImageUrl;
    }
}
