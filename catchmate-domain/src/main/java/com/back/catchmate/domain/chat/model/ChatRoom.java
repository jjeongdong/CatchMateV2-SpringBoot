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
    private Long lastMessageSequence;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    // 채팅방 생성 메서드
    public static ChatRoom createChatRoom(Board board) {
        return ChatRoom.builder()
                .board(board)
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

    // 삭제 메서드
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    // 삭제 여부 확인
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
