package com.back.catchmate.chat.domain;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(
        name = "chat_messages",
        indexes = {
            @Index(
                    name = "idx_chat_messages_room_deleted_id",
                    columnList = "chat_room_id, deleted_at, chat_message_id DESC"),
            @Index(
                    name = "idx_chat_messages_room_type_deleted_id",
                    columnList = "chat_room_id, message_type, deleted_at, chat_message_id DESC")
        })
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

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

    // chatRoom 은 ChatRoomRepository.getReference 의 참조 객체를 받는다 — 메시지마다 방을 SELECT 하지 않기 위해서다.
    private ChatMessage(ChatRoom chatRoom, Long senderId, String content, MessageType messageType, Long sequence) {
        this.chatRoom = chatRoom;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
        this.sequence = sequence;
    }

    public static ChatMessage text(ChatRoom chatRoom, Long senderId, String content, Long sequence) {
        return new ChatMessage(chatRoom, senderId, content, MessageType.TEXT, sequence);
    }

    // 시스템 메시지는 새 순번을 받지 않고 현재 방 시퀀스를 그대로 쓴다 (안 읽은 수에 잡히지 않게).
    public static ChatMessage joined(ChatRoom chatRoom, Long userId, String nickName, Long sequence) {
        return new ChatMessage(chatRoom, userId, nickName + "님이 입장하셨습니다.", MessageType.SYSTEM, sequence);
    }

    public static ChatMessage left(ChatRoom chatRoom, Long userId, String nickName, Long sequence) {
        return new ChatMessage(chatRoom, userId, nickName + "님이 퇴장하셨습니다.", MessageType.SYSTEM, sequence);
    }

    public static ChatMessage kicked(ChatRoom chatRoom, Long userId, String nickName, Long sequence) {
        return new ChatMessage(chatRoom, userId, nickName + "님이 내보내졌습니다.", MessageType.SYSTEM, sequence);
    }
}
