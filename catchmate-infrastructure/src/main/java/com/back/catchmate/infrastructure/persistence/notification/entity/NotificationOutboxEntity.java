package com.back.catchmate.infrastructure.persistence.notification.entity;

import com.back.catchmate.domain.notification.model.NotificationOutbox;
import com.back.catchmate.infrastructure.global.BaseTimeEntity;
import com.back.catchmate.notifications.enums.NotificationChannel;
import com.back.catchmate.notifications.enums.OutboxStatus;
import com.back.catchmate.notifications.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@Table(
        name = "notification_outbox",
        indexes = {
                @Index(
                        name = "idx_outbox_ref",
                        columnList = "recipient_id, reference_type, reference_id, status"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NotificationOutboxEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id")
    private Long recipientId;

    @Column(name = "fcm_token")
    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private NotificationChannel channel;

    private String title;
    private String body;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private int retryCount;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 32)
    private ReferenceType referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    // Domain -> Entity 변환
    public static NotificationOutboxEntity from(NotificationOutbox domain) {
        return NotificationOutboxEntity.builder()
                .id(domain.getId())
                .recipientId(domain.getRecipientId())
                .recipientAddress(domain.getRecipientAddress())
                .channel(domain.getChannel())
                .title(domain.getTitle())
                .body(domain.getBody())
                .payload(domain.getPayload())
                .retryCount(domain.getRetryCount())
                .status(domain.getStatus())
                .referenceType(domain.getReferenceType())
                .referenceId(domain.getReferenceId())
                .build();
    }

    // Entity -> Domain 변환
    public NotificationOutbox toModel() {
        return NotificationOutbox.builder()
                .id(this.id)
                .recipientId(this.recipientId)
                .recipientAddress(this.recipientAddress)
                .channel(this.channel)
                .title(this.title)
                .body(this.body)
                .payload(this.payload)
                .retryCount(this.retryCount)
                .status(this.status)
                .referenceType(this.referenceType)
                .referenceId(this.referenceId)
                .build();
    }
}
