package com.back.catchmate.infrastructure.persistence.notification.entity;

import com.back.catchmate.domain.notification.model.NotificationDelivery;
import com.back.catchmate.infrastructure.global.BaseTimeEntity;
import com.back.catchmate.notifications.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notification_delivery")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDeliveryEntity extends BaseTimeEntity {

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
    private DeliveryStatus status;

    // Domain -> Entity 변환
    public static NotificationDeliveryEntity from(NotificationDelivery domain) {
        NotificationDeliveryEntity entity = new NotificationDeliveryEntity();
        entity.id = domain.getId();
        entity.recipientId = domain.getRecipientId();
        entity.fcmToken = domain.getFcmToken();
        entity.title = domain.getTitle();
        entity.body = domain.getBody();
        entity.payload = domain.getPayload();
        entity.retryCount = domain.getRetryCount();
        entity.status = domain.getStatus();
        return entity;
    }

    // Entity -> Domain 변환
    public NotificationDelivery toModel() {
        return NotificationDelivery.builder()
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
