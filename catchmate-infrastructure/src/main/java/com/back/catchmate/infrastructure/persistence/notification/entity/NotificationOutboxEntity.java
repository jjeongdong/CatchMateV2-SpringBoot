package com.back.catchmate.infrastructure.persistence.notification.entity;

import com.back.catchmate.domain.notification.model.NotificationOutbox;
import com.back.catchmate.infrastructure.global.BaseTimeEntity;
import com.back.catchmate.notifications.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@Table(name = "notification_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationOutboxEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long recipientId;
    private String fcmToken;
    private String title;
    private String body;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private int retryCount;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    // Domain -> Entity 변환
    public static NotificationOutboxEntity from(NotificationOutbox domain) {
        return NotificationOutboxEntity.builder()
                .id(domain.getId())
                .recipientId(domain.getRecipientId())
                .fcmToken(domain.getFcmToken())
                .title(domain.getTitle())
                .body(domain.getBody())
                .payload(domain.getPayload())
                .retryCount(domain.getRetryCount())
                .status(domain.getStatus())
                .build();
    }

    // Entity -> Domain 변환
    public NotificationOutbox toModel() {
        return NotificationOutbox.builder()
                .id(this.id)
                .recipientId(this.recipientId)
                .fcmToken(this.fcmToken)
                .title(this.title)
                .body(this.body)
                .payload(this.payload)
                .retryCount(this.retryCount)
                .status(this.status)
                .build();
    }
}
