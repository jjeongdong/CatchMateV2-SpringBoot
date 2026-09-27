package com.back.catchmate.notification.domain;

import com.back.catchmate.global.persistence.BaseTimeEntity;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    private NotificationOutbox(Long recipientId, String recipientAddress, String title, String body, String payload) {
        this.recipientId = recipientId;
        this.recipientAddress = recipientAddress;
        this.title = title;
        this.body = body;
        this.payload = payload;
        this.retryCount = 0;
        this.status = OutboxStatus.PENDING;
    }

    public static NotificationOutbox create(
            Long recipientId, String recipientAddress, String title, String body, String payload) {
        return new NotificationOutbox(recipientId, recipientAddress, title, body, payload);
    }

    public void startProcessing() {
        this.status = OutboxStatus.PROCESSING;
    }

    public void success() {
        this.status = OutboxStatus.SUCCESS;
    }

    public void permanentFail(String reason) {
        this.status = OutboxStatus.PERMANENT_FAILURE;
        this.errorMessage = reason;
    }

    // 한도에 닿으면 FAILED 로 멈춰 더는 선점되지 않게 한다. 한도 전이면 다음 주기에 다시 선점되도록 PENDING 으로 돌린다.
    public void failRetryable(String errorMessage, int maxRetryCount) {
        this.retryCount++;
        this.errorMessage = errorMessage;
        this.status = retryCount >= maxRetryCount ? OutboxStatus.FAILED : OutboxStatus.PENDING;
    }

    public boolean isFailed() {
        return status == OutboxStatus.FAILED;
    }
}
