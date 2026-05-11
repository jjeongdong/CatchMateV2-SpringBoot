package com.back.catchmate.domain.chat.model;

import com.back.catchmate.domain.board.model.Board;
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
    private Board board;
    private String chatRoomImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    // 채팅방 생성 메서드
    public static ChatRoom createChatRoom(Board board) {
        return ChatRoom.builder()
                .board(board)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 채팅방 이미지 URL 업데이트 메서드
    public void updateImageUrl(String chatRoomImageUrl) {
        this.chatRoomImageUrl = chatRoomImageUrl;
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
