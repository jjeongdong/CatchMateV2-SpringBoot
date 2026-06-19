package com.back.catchmate.chat.adapter.out.persistence.entity;

import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_messages_room_deleted_id",
                columnList = "chat_room_id, deleted_at, chat_message_id DESC"
        ),
        @Index(name = "idx_chat_messages_room_type_deleted_id",
                columnList = "chat_room_id, message_type, deleted_at, chat_message_id DESC"
        )
})
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatMessageEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoomEntity chatRoom;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @Column(nullable = false)
    private Long sequence;

    private LocalDateTime deletedAt;

    public static ChatMessageEntity from(ChatMessage chatMessage) {
        return ChatMessageEntity.builder()
                .id(chatMessage.getId())
                .chatRoom(ChatRoomEntity.builder().id(chatMessage.getChatRoomId()).build())
                .senderId(chatMessage.getSenderId())
                .content(chatMessage.getContent())
                .messageType(chatMessage.getMessageType())
                .sequence(chatMessage.getSequence())
                .deletedAt(chatMessage.getDeletedAt())
                .build();
    }

    public ChatMessage toDomain() {
        return ChatMessage.builder()
                .id(this.id)
                .chatRoomId(this.chatRoom != null ? this.chatRoom.getId() : null)
                .senderId(this.senderId)
                .content(this.content)
                .messageType(this.messageType)
                .createdAt(this.getCreatedAt())
                .sequence(this.sequence)
                .deletedAt(this.deletedAt)
                .build();
    }
}
