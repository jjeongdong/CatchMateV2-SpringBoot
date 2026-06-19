package com.back.catchmate.notification.application.port.out.dto;

/**
 * 채팅방 수신자 정보 (사용자 ID 및 해당 방의 알림 설정 여부)
 */
public record NotificationChatRecipientInfo(
        Long userId,
        boolean isNotificationOn
) {
}
