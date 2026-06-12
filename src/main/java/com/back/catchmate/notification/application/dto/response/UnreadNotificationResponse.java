package com.back.catchmate.notification.application.dto.response;


public record UnreadNotificationResponse(
        boolean hasUnread
) {
    public static UnreadNotificationResponse of(boolean hasUnread) {
        return new UnreadNotificationResponse(hasUnread);
    }
}
