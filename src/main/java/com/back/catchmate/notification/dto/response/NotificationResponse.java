package com.back.catchmate.notification.dto.response;

import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String title,
        String alarmType,
        boolean read,
        LocalDateTime createdAt,
        String senderProfileImageUrl,
        String senderNickname,
        Long targetId,
        Long boardId,
        String gameInfo,
        String acceptStatus
) {
    public static NotificationResponse from(Notification notification, UserSummary sender, String acceptStatus, String gameInfo) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getType() != null ? notification.getType().name() : null,
                notification.isRead(),
                notification.getCreatedAt(),
                sender != null ? sender.profileImageUrl() : null,
                sender != null ? sender.nickName() : null,
                notification.getTargetId(),
                notification.getBoardId(),
                gameInfo,
                acceptStatus
        );
    }
}
