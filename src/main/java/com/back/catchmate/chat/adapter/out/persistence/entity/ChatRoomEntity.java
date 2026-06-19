package com.back.catchmate.chat.adapter.out.persistence.entity;

import com.back.catchmate.chat.domain.model.ChatRoom;
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
public class ChatRoomEntity extends BaseTimeEntity {
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

    public static ChatRoomEntity from(ChatRoom chatRoom) {
        return ChatRoomEntity.builder()
                .id(chatRoom.getId())
                .boardId(chatRoom.getBoardId())
                .lastMessageSequence(chatRoom.getLastMessageSequence())
                .chatRoomImageUrl(chatRoom.getChatRoomImageUrl())
                .deletedAt(chatRoom.getDeletedAt())
                .build();
    }

    public ChatRoom toDomain() {
        return ChatRoom.builder()
                .id(this.id)
                .boardId(this.boardId)
                .lastMessageSequence(this.lastMessageSequence)
                .chatRoomImageUrl(this.chatRoomImageUrl)
                .createdAt(this.getCreatedAt())
                .deletedAt(this.deletedAt)
                .build();
    }
}
