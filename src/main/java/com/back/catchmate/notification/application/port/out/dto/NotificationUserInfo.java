package com.back.catchmate.notification.application.port.out.dto;

public record NotificationUserInfo(
        Long userId,
        String nickName,
        String profileImageUrl,
        String fcmToken,
        boolean chatAlarmEnabled,
        boolean enrollAlarmEnabled,
        boolean eventAlarmEnabled
) {
}
