package com.back.catchmate.notification.entity;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import com.back.catchmate.notification.entity.enums.AlarmType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    public static Notification createNotification(
            Long userId, Long senderId, Long boardId, String title, AlarmType type, Long targetId) {
        return Notification.builder()
                .userId(userId)
                .senderId(senderId)
                .boardId(boardId)
                .title(title)
                .type(type)
                .targetId(targetId)
                .read(false)
                .build();
    }

    public void markAsRead() {
        this.read = true;
    }
}
