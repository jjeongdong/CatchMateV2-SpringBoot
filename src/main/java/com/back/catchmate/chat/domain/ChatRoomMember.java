package com.back.catchmate.chat.domain;

import com.back.catchmate.chat.domain.exception.ChatRoomReentryNotAllowedException;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Getter
@Table(
        name = "chat_room_members",
        indexes = {
            // 내 채팅방 목록(JOIN chat_room_members ON ... WHERE user_id = ? AND left_at IS NULL) 용.
            // chat_room_id 는 FK 인덱스가 자동 생성되지만 user_id 는 없어 풀스캔이었다.
            @Index(name = "idx_chat_room_members_user_active", columnList = "user_id, left_at")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "read_only_at")
    private LocalDateTime readOnlyAt;

    // 읽음 처리는 Redis 버퍼를 모아 JDBC 배치로 올린다 (ChatRoomMemberRepository.updateLastReadSequences).
    @Column(name = "last_read_sequence", nullable = false)
    private Long lastReadSequence;

    @Column(name = "is_notification_on", nullable = false)
    private boolean isNotificationOn;

    private ChatRoomMember(ChatRoom chatRoom, Long userId, Long lastReadSequence, LocalDateTime now) {
        this.chatRoom = chatRoom;
        this.userId = userId;
        this.lastReadSequence = lastReadSequence;
        this.joinedAt = now;
        this.isNotificationOn = true;
    }

    // 참여 전 메시지는 읽은 것으로 친다 (lastReadSequence = 참여 시점의 방 시퀀스).
    public static ChatRoomMember create(ChatRoom chatRoom, Long userId, Long lastReadSequence, LocalDateTime now) {
        return new ChatRoomMember(chatRoom, userId, lastReadSequence, now);
    }

    // 이미 멤버면 새 매칭으로 보고 읽기 전용만 푼다. 스스로 나갔거나 내보내진 방에는 다시 들어올 수 없다.
    public void rejoin() {
        if (!isActive()) {
            throw new ChatRoomReentryNotAllowedException();
        }
        this.readOnlyAt = null;
    }

    public void leave(LocalDateTime now) {
        this.leftAt = now;
    }

    public void enableNotification() {
        this.isNotificationOn = true;
    }

    public void disableNotification() {
        this.isNotificationOn = false;
    }

    // 차단 등으로 전송만 막는다. 아직 호출하는 곳이 없다 (spec §8 부채).
    public void markAsReadOnly(LocalDateTime now) {
        if (this.readOnlyAt == null) {
            this.readOnlyAt = now;
        }
    }

    public boolean isActive() {
        return this.leftAt == null;
    }

    public boolean isReadOnly() {
        return this.readOnlyAt != null;
    }

    public Long calculateUnreadCount(Long currentRoomSequence) {
        long count = currentRoomSequence - this.lastReadSequence;
        return count < 0 ? 0 : count;
    }

    public MembershipSnapshot snapshot() {
        return new MembershipSnapshot(isActive(), isReadOnly());
    }
}
