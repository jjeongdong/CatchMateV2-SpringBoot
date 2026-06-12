package com.back.catchmate.notification.application.dto.response;

import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.notification.domain.model.Notification;
import com.back.catchmate.user.domain.enums.AlarmType;
import com.back.catchmate.user.domain.model.User;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String title,
        AlarmType alarmType,
        boolean read,
        LocalDateTime createdAt,
        String senderProfileImageUrl,
        String senderNickname,
        Long targetId,
        Long boardId,
        String gameInfo,
        AcceptStatus acceptStatus
) {
    public static NotificationResponse from(Notification notification, User sender, AcceptStatus acceptStatus, String gameInfo) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt(),
                sender != null ? sender.getProfileImageUrl() : null,
                sender != null ? sender.getNickName() : null,
                notification.getTargetId(),
                notification.getBoardId(),
                gameInfo,
                acceptStatus
        );
    }
}
