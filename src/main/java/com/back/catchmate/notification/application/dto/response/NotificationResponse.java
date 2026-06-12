package com.back.catchmate.notification.application.dto.response;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.notification.domain.model.Notification;
import com.back.catchmate.user.domain.enums.AlarmType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    public static NotificationResponse from(Notification notification) {
        return from(notification, null);
    }

    public static NotificationResponse from(Notification notification, AcceptStatus acceptStatus) {
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
                notification.getBoard() != null ?
                        formatGameInfo(notification.getBoard()) : null,
                acceptStatus
        );
    }

    private static String formatGameInfo(Board board) {
        Game game = board.getGame();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        return String.format("%s · %s · %s vs %s",
                game.getGameStartDate().format(formatter),
                game.getLocation(),
                game.getHomeClub().getName(),
                game.getAwayClub().getName()
        );
    }
}
