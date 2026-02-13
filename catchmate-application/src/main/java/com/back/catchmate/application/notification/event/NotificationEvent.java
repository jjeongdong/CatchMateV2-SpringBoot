package com.back.catchmate.application.notification.event;

import java.util.Map;

/**
 * @param userId 알림 받을 유저 ID
 * @param data   알림 데이터
 */
public record NotificationEvent(
        Long userId,
        Map<String, String> data
) {
    public static NotificationEvent of(Long userId, Map<String, String> data) {
        return new NotificationEvent(userId, data);
    }
}
