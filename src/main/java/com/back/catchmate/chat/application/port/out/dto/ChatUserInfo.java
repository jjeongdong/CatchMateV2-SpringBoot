package com.back.catchmate.chat.application.port.out.dto;

public record ChatUserInfo(
        Long userId,
        String nickName,
        String profileImageUrl,
        String fcmToken,
        boolean chatAlarmEnabled,
        Long clubId
) {
}
