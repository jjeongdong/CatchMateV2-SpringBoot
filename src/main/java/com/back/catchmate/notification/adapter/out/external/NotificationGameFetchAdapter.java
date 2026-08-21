package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.game.service.GameService;
import com.back.catchmate.notification.application.port.out.dto.NotificationGameInfo;
import com.back.catchmate.notification.application.port.out.external.GameFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationGameFetchAdapter implements GameFetchPort {
    private final GameService gameService;

    @Override
    public NotificationGameInfo getGame(Long gameId) {
        return toNotificationGameInfo(gameService.getGameSummary(gameId));
    }

    @Override
    public List<NotificationGameInfo> getGames(List<Long> gameIds) {
        return gameService.getGameSummaries(gameIds).stream()
                .map(this::toNotificationGameInfo)
                .collect(Collectors.toList());
    }

    private NotificationGameInfo toNotificationGameInfo(GameSummary response) {
        if (response == null) return null;
        return new NotificationGameInfo(
                response.gameId(),
                response.gameStartDate(),
                response.location(),
                response.homeClubId(),
                response.awayClubId()
        );
    }
}
