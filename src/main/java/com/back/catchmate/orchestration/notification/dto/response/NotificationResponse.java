package com.back.catchmate.orchestration.notification.dto.response;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.enroll.model.AcceptStatus;
import com.back.catchmate.domain.game.model.Game;
import com.back.catchmate.domain.notification.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import com.back.catchmate.user.enums.AlarmType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String title;
    private AlarmType alarmType;
    private boolean read;
    private LocalDateTime createdAt;
    private String senderProfileImageUrl;
    private String senderNickname;
    private Long targetId;
    private Long boardId;
    private String gameInfo;
    private AcceptStatus acceptStatus;

    public static NotificationResponse from(Notification notification) {
        return from(notification, null);
    }

    public static NotificationResponse from(Notification notification, AcceptStatus acceptStatus) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .alarmType(notification.getType())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .senderProfileImageUrl(notification.getSender() != null ?
                        notification.getSender().getProfileImageUrl() : null)
                .senderNickname(notification.getSender() != null ?
                        notification.getSender().getNickName() : null)
                .targetId(notification.getTargetId())
                .boardId(notification.getBoard() != null ?
                        notification.getBoard().getId() : null)
                .gameInfo(notification.getBoard() != null ?
                        formatGameInfo(notification.getBoard()) : null)
                .acceptStatus(acceptStatus)
                .build();
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
