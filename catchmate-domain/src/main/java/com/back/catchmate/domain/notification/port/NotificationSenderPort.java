package com.back.catchmate.domain.notification.port;

import com.back.catchmate.notifications.enums.NotificationChannel;

import java.util.List;
import java.util.Map;

public interface NotificationSenderPort {
    void sendNotification(String token, String title, String body, Map<String, String> data);

    void sendMulticastNotification(List<String> tokens, String title, String body, Map<String, String> data);

    /**
     * 오프라인 사용자에게만 알림 전송 (온라인 상태 체크 포함)
     */
    void sendNotificationIfOffline(Long userId, String token, String title, String body, Map<String, String> data);

    NotificationChannel getChannel();

    default boolean supports(NotificationChannel channel) {
        return getChannel() == channel;
    }
}
