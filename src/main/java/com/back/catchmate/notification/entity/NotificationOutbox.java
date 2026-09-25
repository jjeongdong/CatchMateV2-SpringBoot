package com.back.catchmate.notification.entity;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import com.back.catchmate.notification.entity.enums.OutboxStatus;
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

/**
 * 알림 아웃박스 행.
 * <p>
 * {@code createdAt}({@link BaseTimeEntity}) 은 PENDING 최초 적재 시각이며,
 * PENDING→SUCCESS 지연 측정의 기준점으로만 사용한다. (불변)
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "notification_outbox",
        indexes = {
            @Index(name = "idx_outbox_status_retry", columnList = "status, retry_count"),
            @Index(name = "idx_outbox_recipient_status", columnList = "recipient_id, status")
        })
public class NotificationOutbox extends BaseTimeEntity {

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

    public static NotificationOutbox create(
            Long recipientId, String recipientAddress, String title, String body, String payload) {
        return NotificationOutbox.builder()
                .recipientId(recipientId)
                .recipientAddress(recipientAddress)
                .title(title)
                .body(body)
                .payload(payload)
                .retryCount(0)
                .status(OutboxStatus.PENDING)
                .errorMessage(null)
                .build();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void recordError(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void startProcessing() {
        this.status = OutboxStatus.PROCESSING;
    }

    public void pending() {
        this.status = OutboxStatus.PENDING;
    }

    public void fail() {
        this.status = OutboxStatus.FAILED;
    }

    public void success() {
        this.status = OutboxStatus.SUCCESS;
    }

    public void permanentFail(String reason) {
        this.status = OutboxStatus.PERMANENT_FAILURE;
        this.errorMessage = reason;
    }
}
