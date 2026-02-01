package com.back.catchmate.domain.notification.port;

import java.util.List;
import java.util.Map;

public interface NotificationSender {
    void sendNotification(String token, String title, String body, Map<String, String> data);
    void sendMulticastNotification(List<String> tokens, String title, String body, Map<String, String> data);

    /**
     * 오프라인 사용자에게만 알림 전송 (온라인 상태 체크 포함)
     */
    void sendNotificationIfOffline(Long userId, String token, String title, String body, Map<String, String> data);
}
