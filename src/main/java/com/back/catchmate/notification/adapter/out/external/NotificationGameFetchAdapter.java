package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.game.domain.service.GameService;
import com.back.catchmate.notification.application.port.out.GameFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationGameFetchAdapter implements GameFetchPort {
    private final GameService gameService;

    @Override
    public Game getGame(Long gameId) {
        return gameService.getGame(gameId);
    }

    @Override
    public List<Game> getGames(List<Long> gameIds) {
        return gameService.getGames(gameIds);
    }
}
