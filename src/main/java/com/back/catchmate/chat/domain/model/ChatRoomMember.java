package com.back.catchmate.chat.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {
    private Long id;
    private Long chatRoomId;
    private Long userId;
    private Long lastReadSequence;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private LocalDateTime readOnlyAt;
    private boolean isNotificationOn;

    /**
     * 채팅방 멤버 생성 메서드
     */
    public static ChatRoomMember create(Long chatRoomId, Long userId, Long initialLastReadSequence) {
        return ChatRoomMember.builder()
                .chatRoomId(chatRoomId)
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
