package com.back.catchmate.notification.adapter.out.persistence.entity;

import com.back.catchmate.notification.domain.model.NotificationOutbox;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import com.back.catchmate.notification.domain.model.OutboxStatus;
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
@Table(name = "notification_outbox")
public class NotificationOutboxEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long recipientId;

    @Column(name = "fcm_token")
    private String recipientAddress;

    private String title;
    private String body;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private int retryCount;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public static NotificationOutboxEntity from(NotificationOutbox domain) {
        return NotificationOutboxEntity.builder()
                .id(domain.getId())
                .recipientId(domain.getRecipientId())
                .recipientAddress(domain.getRecipientAddress())
                .title(domain.getTitle())
                .body(domain.getBody())
                .payload(domain.getPayload())
                .retryCount(domain.getRetryCount())
                .status(domain.getStatus())
                .errorMessage(domain.getErrorMessage())
                .build();
    }

    public NotificationOutbox toDomain() {
        return NotificationOutbox.builder()
                .id(this.id)
                .recipientId(this.recipientId)
                .recipientAddress(this.recipientAddress)
                .title(this.title)
                .body(this.body)
                .payload(this.payload)
                .retryCount(this.retryCount)
                .status(this.status)
                .errorMessage(this.errorMessage)
                .createdAt(this.getCreatedAt())
                .build();
    }
}
