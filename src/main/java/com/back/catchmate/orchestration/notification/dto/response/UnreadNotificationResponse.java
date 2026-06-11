package com.back.catchmate.orchestration.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UnreadNotificationResponse {
    private boolean hasUnread;

    public static UnreadNotificationResponse of(boolean hasUnread) {
        return new UnreadNotificationResponse(hasUnread);
    }
}
