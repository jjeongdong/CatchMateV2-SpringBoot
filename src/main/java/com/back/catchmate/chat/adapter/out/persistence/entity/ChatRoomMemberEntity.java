package com.back.catchmate.chat.adapter.out.persistence.entity;

import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@Table(name = "chat_room_members", indexes = {
        // 내 채팅방 목록(JOIN chat_room_members ON ... WHERE user_id = ? AND left_at IS NULL) 용.
        // chat_room_id 는 FK 인덱스가 자동 생성되지만 user_id 는 없어 풀스캔이었다.
        @Index(name = "idx_chat_room_members_user_active",
                columnList = "user_id, left_at"
        )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatRoomMemberEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoomEntity chatRoom;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "read_only_at")
    private LocalDateTime readOnlyAt;

    @Column(name = "last_read_sequence", nullable = false)
    private Long lastReadSequence;

    @Column(name = "is_notification_on", nullable = false)
    @Builder.Default
    private boolean isNotificationOn = true;

    public static ChatRoomMemberEntity from(ChatRoomMember member) {
        return ChatRoomMemberEntity.builder()
                .id(member.getId())
                .chatRoom(ChatRoomEntity.builder().id(member.getChatRoomId()).build())
                .userId(member.getUserId())
                .joinedAt(member.getJoinedAt())
                .leftAt(member.getLeftAt())
                .readOnlyAt(member.getReadOnlyAt())
                .lastReadSequence(member.getLastReadSequence())
                .isNotificationOn(member.isNotificationOn())
                .build();
    }

    public ChatRoomMember toDomain() {
        return ChatRoomMember.builder()
                .id(this.id)
                .chatRoomId(this.chatRoom != null ? this.chatRoom.getId() : null)
                .userId(this.userId)
                .joinedAt(this.joinedAt)
                .leftAt(this.leftAt)
                .readOnlyAt(this.readOnlyAt)
                .lastReadSequence(this.lastReadSequence)
                .isNotificationOn(this.isNotificationOn)
                .build();
    }
}
