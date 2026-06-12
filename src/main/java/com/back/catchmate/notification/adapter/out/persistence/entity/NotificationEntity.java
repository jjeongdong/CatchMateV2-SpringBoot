package com.back.catchmate.notification.adapter.out.persistence.entity;

import com.back.catchmate.notification.domain.model.Notification;
import com.back.catchmate.global.infrastructure.BaseTimeEntity;
import com.back.catchmate.board.adapter.out.persistence.entity.BoardEntity;
import com.back.catchmate.user.adapter.out.persistence.entity.UserEntity;
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
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import com.back.catchmate.user.domain.enums.AlarmType;

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
    @NotFound(action = NotFoundAction.IGNORE)
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private BoardEntity board;

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
                .user(UserEntity.builder().id(notification.getUserId()).build())
                .sender(notification.getSenderId() != null ? UserEntity.builder().id(notification.getSenderId()).build() : null)
                .board(notification.getBoardId() != null ? BoardEntity.builder().id(notification.getBoardId()).build() : null)
                .title(notification.getTitle())
                .type(notification.getType())
                .read(notification.isRead())
                .targetId(notification.getTargetId())
                .build();
    }

    public Notification toModel() {
        return Notification.builder()
                .id(this.id)
                .userId(this.user != null ? this.user.getId() : null)
                .senderId(this.sender != null ? this.sender.getId() : null)
                .boardId(this.board != null ? this.board.getId() : null)
                .title(this.title)
                .type(this.type)
                .read(this.read)
                .targetId(this.targetId)
                .createdAt(this.getCreatedAt())
                .build();
    }
}
