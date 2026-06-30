package com.back.catchmate.notification.application.port.out.external;

import java.util.Map;

/*
 * 사용자에게 PUSH 알림을 전송하기 위한 출력 포트.
 */
public interface NotificationSenderPort {
    void sendNotification(Long userId, String token, String title, String body, Map<String, String> data);
}
