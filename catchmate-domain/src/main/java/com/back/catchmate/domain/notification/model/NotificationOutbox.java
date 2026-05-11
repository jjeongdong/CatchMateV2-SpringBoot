package com.back.catchmate.domain.notification.model;

import com.back.catchmate.notifications.enums.NotificationChannel;
import com.back.catchmate.notifications.enums.OutboxStatus;
import com.back.catchmate.notifications.enums.ReferenceType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationOutbox {
    private Long id;
    private Long recipientId;
    private String recipientAddress;
    private NotificationChannel channel;
    private String title;
    private String body;
    private String payload;
    private int retryCount;
    private OutboxStatus status;
    private ReferenceType referenceType;
    private Long referenceId;

    public static NotificationOutbox create(Long recipientId, String recipientAddress, NotificationChannel channel,
                                            String title, String body, String payload,
                                            ReferenceType referenceType, Long referenceId) {
        return NotificationOutbox.builder()
                .recipientId(recipientId)
                .recipientAddress(recipientAddress)
                .channel(channel)
                .title(title)
                .body(body)
                .payload(payload)
                .retryCount(0)
                .status(OutboxStatus.PENDING)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();
    }

    public void incrementRetryCount() {
        this.retryCount++;
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

    public void skip() {
        this.status = OutboxStatus.SKIPPED;
    }
}
