package com.back.catchmate.notification.application.dto.response;

import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.notification.domain.model.Notification;
import com.back.catchmate.user.domain.enums.AlarmType;

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
    public static NotificationResponse from(Notification notification, AcceptStatus acceptStatus, String gameInfo) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getSender() != null ?
                        notification.getSender().getProfileImageUrl() : null,
                notification.getSender() != null ?
                        notification.getSender().getNickName() : null,
                notification.getTargetId(),
                notification.getBoard() != null ?
                        notification.getBoard().getId() : null,
                gameInfo,
                acceptStatus
        );
    }
}
