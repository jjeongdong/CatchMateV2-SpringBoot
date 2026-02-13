package com.back.catchmate.domain.chat.model;

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
public class ChatRoomMember {
    private Long id;
    private ChatRoom chatRoom;
    private User user;
    private Long lastReadSequence;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;

    /**
     * 채팅방 멤버 생성 메서드
     */
    public static ChatRoomMember create(ChatRoom chatRoom, User user) {
        return ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .user(user)
                .lastReadSequence(chatRoom.getLastMessageSequence())
                .joinedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 읽음 처리 메서드
     */
    public void updateLastReadSequence(Long currentRoomSequence) {
        if (currentRoomSequence > this.lastReadSequence) {
            this.lastReadSequence = currentRoomSequence;
        }
    }

    /**
     * 채팅방 퇴장
     */
    public void leave() {
        this.leftAt = LocalDateTime.now();
    }

    /**
     * 현재 채팅방에 참가중인지 확인
     */
    public boolean isActive() {
        return this.leftAt == null;
    }

    /**
     * 읽지 않은 메시지 수 계산
     */
    public Long calculateUnreadCount(Long currentRoomSequence) {
        long count = currentRoomSequence - this.lastReadSequence;
        return count < 0 ? 0 : count;
    }
}
