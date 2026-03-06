package com.back.catchmate.domain.notification.model;

import com.back.catchmate.notifications.enums.OutboxStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationOutbox {
    private Long id;
    private Long recipientId;
    private String fcmToken;
    private String title;
    private String body;
    private String payload;
    private int retryCount;
    private OutboxStatus status;

    public static NotificationOutbox create(Long recipientId, String fcmToken, String title, String body, String payload) {
        return NotificationOutbox.builder()
                .recipientId(recipientId)
                .fcmToken(fcmToken)
                .title(title)
                .body(body)
                .payload(payload)
                .retryCount(0)
                .status(OutboxStatus.PENDING)
                .build();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void fail() {
        this.status = OutboxStatus.FAILED;
    }

    public void success() {
        this.status = OutboxStatus.SUCCESS;
    }
}
