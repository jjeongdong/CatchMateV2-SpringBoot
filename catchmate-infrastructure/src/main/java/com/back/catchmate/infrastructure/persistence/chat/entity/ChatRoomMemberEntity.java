package com.back.catchmate.infrastructure.persistence.chat.entity;

import com.back.catchmate.domain.chat.model.ChatRoomMember;
import com.back.catchmate.infrastructure.global.BaseTimeEntity;
import com.back.catchmate.infrastructure.persistence.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@Table(name = "chat_room_members")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "last_read_sequence", nullable = false)
    private Long lastReadSequence;

    public static ChatRoomMemberEntity from(ChatRoomMember member) {
        return ChatRoomMemberEntity.builder()
                .id(member.getId())
                .chatRoom(ChatRoomEntity.from(member.getChatRoom()))
                .user(UserEntity.from(member.getUser()))
                .joinedAt(member.getJoinedAt())
                .leftAt(member.getLeftAt())
                .lastReadSequence(member.getLastReadSequence())
                .build();
    }

    public ChatRoomMember toModel() {
        return ChatRoomMember.builder()
                .id(this.id)
                .chatRoom(this.chatRoom.toModel())
                .user(this.user.toModel())
                .joinedAt(this.joinedAt)
                .leftAt(this.leftAt)
                .lastReadSequence(this.lastReadSequence)
                .build();
    }
}
