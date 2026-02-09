package com.back.catchmate.domain.notification.model;

import com.back.catchmate.notifications.enums.DeliveryStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationDelivery {
    private Long id;
    private Long recipientId;
    private String fcmToken;
    private String title;
    private String body;
    private String payload;
    private int retryCount;
    private DeliveryStatus status;

    public static NotificationDelivery create(Long recipientId, String fcmToken, String title, String body, String payload) {
        return NotificationDelivery.builder()
                .recipientId(recipientId)
                .fcmToken(fcmToken)
                .title(title)
                .body(body)
                .payload(payload)
                .retryCount(0)
                .status(DeliveryStatus.PENDING)
                .build();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void fail() {
        this.status = DeliveryStatus.FAILED;
    }

    public void success() {
        this.status = DeliveryStatus.SUCCESS;
    }
}
