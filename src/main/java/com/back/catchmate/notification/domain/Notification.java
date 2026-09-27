package com.back.catchmate.notification.domain;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import com.back.catchmate.notification.domain.exception.NotificationNotOwnerException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notifications",
        indexes = {
            // 알림 목록 조회(user_id 필터 + created_at DESC 정렬)와 안 읽음 여부 확인 용.
            @Index(name = "idx_notifications_user_created", columnList = "user_id, created_at DESC")
        })
public class Notification extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "board_id")
    private Long boardId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlarmType type;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column
    private Long targetId;

    private Notification(Long userId, Long senderId, Long boardId, String title, AlarmType type, Long targetId) {
        this.userId = userId;
        this.senderId = senderId;
        this.boardId = boardId;
        this.title = title;
        this.type = type;
        this.targetId = targetId;
        this.read = false;
    }

    public static Notification create(
            Long userId, Long senderId, Long boardId, String title, AlarmType type, Long targetId) {
        return new Notification(userId, senderId, boardId, title, type, targetId);
    }

    public void verifyOwner(Long userId) {
        if (!Objects.equals(this.userId, userId)) {
            throw new NotificationNotOwnerException();
        }
    }

    public void markAsRead() {
        this.read = true;
    }

    // 신청 알림의 targetId 는 신청 id 라서, 화면에 수락 상태를 붙이려면 신청을 조회해야 한다.
    public boolean isEnroll() {
        return type == AlarmType.ENROLL && targetId != null;
    }
}
