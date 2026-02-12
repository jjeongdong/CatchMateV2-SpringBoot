package com.back.catchmate.infrastructure.persistence.chat.entity;

import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.infrastructure.global.BaseTimeEntity;
import com.back.catchmate.infrastructure.persistence.board.entity.BoardEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "chat_rooms")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatRoomEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

    @Column(name = "last_message_sequence", nullable = false)
    private Long lastMessageSequence;

    private LocalDateTime deletedAt;

    public static ChatRoomEntity from(ChatRoom chatRoom) {
        return ChatRoomEntity.builder()
                .id(chatRoom.getId())
                .board(BoardEntity.from(chatRoom.getBoard()))
                .lastMessageSequence(chatRoom.getLastMessageSequence())
                .deletedAt(chatRoom.getDeletedAt())
                .build();
    }

    public ChatRoom toModel() {
        return ChatRoom.builder()
                .id(this.id)
                .board(this.board.toModel())
                .lastMessageSequence(this.lastMessageSequence)
                .createdAt(this.getCreatedAt())
                .deletedAt(this.deletedAt)
                .build();
    }
}
