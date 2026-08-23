package com.back.catchmate.chat.entity;

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

    @Column(name = "last_read_sequence", nullable = false)
    private Long lastReadSequence;

    @Column(name = "is_notification_on", nullable = false)
    @Builder.Default
    private boolean isNotificationOn = true;

    /**
     * 채팅방 멤버 생성 메서드
     */
    public static ChatRoomMember create(Long chatRoomId, Long userId, Long initialLastReadSequence) {
        return ChatRoomMember.builder()
                .chatRoom(ChatRoom.builder().id(chatRoomId).build())
                .userId(userId)
                .lastReadSequence(initialLastReadSequence)
                .joinedAt(LocalDateTime.now())
                .isNotificationOn(true)
                .build();
    }

    /**
     * 읽음 처리 메서드
     */
    public void updateLastReadSequence(Long currentRoomSequence) {
        if (currentRoomSequence > this.lastReadSequence) {
            this.lastReadSequence = currentRoomSequence;
        }
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

    /**
     * 읽지 않은 메시지 수 계산
     */
    public Long calculateUnreadCount(Long currentRoomSequence) {
        long count = currentRoomSequence - this.lastReadSequence;
        return count < 0 ? 0 : count;
    }

    /**
     * 알림 설정 업데이트
     */
    public void updateNotificationSetting(boolean isOn) {
        this.isNotificationOn = isOn;
    }

    /**
     * 차단 등으로 인해 읽기 전용 상태로 전환
     */
    public void markAsReadOnly() {
        if (this.readOnlyAt == null) {
            this.readOnlyAt = LocalDateTime.now();
        }
    }

    /**
     * 새 매칭 등으로 읽기 전용 상태 해제
     */
    public void clearReadOnly() {
        this.readOnlyAt = null;
    }

    public boolean isReadOnly() {
        return this.readOnlyAt != null;
    }
}
