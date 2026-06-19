package com.back.catchmate.notification.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationOutbox {
    private Long id;
    private Long recipientId;
    private String recipientAddress;
    private String title;
    private String body;
    private String payload;
    private int retryCount;
    private OutboxStatus status;
    private String errorMessage;

    public static NotificationOutbox create(Long recipientId, String recipientAddress, String title, String body, String payload) {
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
