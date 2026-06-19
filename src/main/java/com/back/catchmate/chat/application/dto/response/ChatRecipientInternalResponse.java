package com.back.catchmate.chat.application.dto.response;

public record ChatRecipientInternalResponse(
        Long userId,
        boolean isNotificationOn
) {
}
