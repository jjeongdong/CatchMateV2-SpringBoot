package com.back.catchmate.chat.dto.response;

public record ChatRecipientSummary(
        Long userId,
        boolean isNotificationOn
) {
}
