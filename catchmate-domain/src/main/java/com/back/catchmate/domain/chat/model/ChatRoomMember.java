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
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;

    /**
     * 채팅방 멤버 생성 메서드
     */
    public static ChatRoomMember create(ChatRoom chatRoom, User user) {
        return ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .user(user)
                .joinedAt(LocalDateTime.now())
                .build();
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
}
