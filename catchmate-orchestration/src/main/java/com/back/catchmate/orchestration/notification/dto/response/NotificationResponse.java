package com.back.catchmate.orchestration.notification.dto.response;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.game.model.Game;
import com.back.catchmate.domain.notification.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import user.enums.AlarmType;

import java.time.LocalDateTime;

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
    private String gameInfo;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .alarmType(notification.getType())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .senderProfileImageUrl(notification.getSender() != null ?
                        notification.getSender().getProfileImageUrl() : null)
                .gameInfo(notification.getBoard() != null ?
                        formatGameInfo(notification.getBoard()) : null)
                .build();
    }

    private static String formatGameInfo(Board board) {
        Game game = board.getGame();
        return String.format("%s · %s · %s vs %s",
                game.getGameStartDate().toLocalDate(),
                game.getLocation(),
                game.getHomeClub().getName(),
                game.getAwayClub().getName()
        );
    }
}
