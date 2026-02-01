package com.back.catchmate.infrastructure.persistence.notification.entity;

import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.infrastructure.global.BaseTimeEntity;
import com.back.catchmate.infrastructure.persistence.board.entity.BoardEntity;
import com.back.catchmate.infrastructure.persistence.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import user.enums.AlarmType;

@Entity
@Table(name = "notifications")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private BoardEntity board;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlarmType type;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    public static NotificationEntity from(Notification notification) {
        return NotificationEntity.builder()
                .id(notification.getId())
                .user(UserEntity.from(notification.getUser()))
                .sender(notification.getSender() != null ? UserEntity.from(notification.getSender()) : null)
                .board(notification.getBoard() != null ? BoardEntity.from(notification.getBoard()) : null)
                .title(notification.getTitle())
                .type(notification.getType())
                .read(notification.isRead())
                .build();
    }

    public Notification toModel() {
        return Notification.builder()
                .id(this.id)
                .user(this.user.toModel())
                .sender(this.sender != null ? this.sender.toModel() : null)
                .board(this.board != null ? this.board.toModel() : null)
                .title(this.title)
                .type(this.type)
                .read(this.read)
                .createdAt(this.getCreatedAt())
                .build();
    }
}
