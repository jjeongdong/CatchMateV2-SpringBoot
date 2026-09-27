package com.back.catchmate.notification.application.dto.result;

import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

// 필드 이름·순서는 옛 NotificationResponse 그대로 (앱 호환).
public record NotificationResult(
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
        String acceptStatus) {

    public static NotificationResult of(
            Notification notification, UserInfo sender, String acceptStatus, String gameInfo) {
        return new NotificationResult(
                notification.getId(),
                notification.getTitle(),
                notification.getType().name(),
                notification.isRead(),
                notification.getCreatedAt(),
                sender != null ? sender.profileImageUrl() : null,
                sender != null ? sender.nickName() : null,
                notification.getTargetId(),
                notification.getBoardId(),
                gameInfo,
                acceptStatus);
    }
}
