package com.back.catchmate.chat.entity;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@Table(name = "chat_rooms",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_rooms_board_id",
                        columnNames = {"board_id"}
                )
        }
)
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatRoom extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "last_message_sequence", nullable = false)
    private Long lastMessageSequence;

    @Column(name = "chat_room_image_url")
    private String chatRoomImageUrl;

    private LocalDateTime deletedAt;

    // 채팅방 생성 메서드
    public static ChatRoom createChatRoom(Long boardId) {
        return ChatRoom.builder()
                .boardId(boardId)
                .lastMessageSequence(0L)
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
