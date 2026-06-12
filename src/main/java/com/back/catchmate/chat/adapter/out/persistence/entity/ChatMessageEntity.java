package com.back.catchmate.chat.adapter.out.persistence.entity;

import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.global.infrastructure.BaseTimeEntity;
import com.back.catchmate.user.adapter.out.persistence.entity.UserEntity;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

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
                .sender(UserEntity.builder().id(chatMessage.getSenderId()).build())
                .content(chatMessage.getContent())
                .messageType(chatMessage.getMessageType())
                .sequence(chatMessage.getSequence())
                .deletedAt(chatMessage.getDeletedAt())
                .build();
    }

    public ChatMessage toModel() {
        return ChatMessage.builder()
                .id(this.id)
                .chatRoomId(this.chatRoom != null ? this.chatRoom.getId() : null)
                .senderId(this.sender != null ? this.sender.getId() : null)
                .content(this.content)
                .messageType(this.messageType)
                .createdAt(this.getCreatedAt())
                .sequence(this.sequence)
                .deletedAt(this.deletedAt)
                .build();
    }
}
