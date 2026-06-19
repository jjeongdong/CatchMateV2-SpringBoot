package com.back.catchmate.notification.adapter.out.persistence.entity;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import com.back.catchmate.notification.domain.model.AlarmType;
import com.back.catchmate.notification.domain.model.Notification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "notifications")
public class NotificationEntity extends BaseTimeEntity {
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

    public static NotificationEntity from(Notification notification) {
        return NotificationEntity.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .senderId(notification.getSenderId())
                .boardId(notification.getBoardId())
                .title(notification.getTitle())
                .type(notification.getType())
                .read(notification.isRead())
                .targetId(notification.getTargetId())
                .build();
    }

    public Notification toDomain() {
        return Notification.builder()
                .id(this.id)
                .userId(this.userId)
                .senderId(this.senderId)
                .boardId(this.boardId)
                .title(this.title)
                .type(this.type)
                .read(this.read)
                .targetId(this.targetId)
                .createdAt(this.getCreatedAt())
                .build();
    }
}
