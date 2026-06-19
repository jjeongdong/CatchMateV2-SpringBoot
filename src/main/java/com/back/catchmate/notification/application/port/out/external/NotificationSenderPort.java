package com.back.catchmate.notification.application.port.out.external;

import java.util.Map;

public interface NotificationSenderPort {
    /**
     * 알림 전송
     */
    void sendNotification(Long userId, String token, String title, String body, Map<String, String> data);
}
