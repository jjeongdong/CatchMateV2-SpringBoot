package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.GameFetchPort;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.game.domain.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardGameFetchAdapter implements GameFetchPort {
    private final GameService gameService;

    @Override
    public Game findOrCreateGame(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate, String location) {
        return gameService.findOrCreateGame(homeClubId, awayClubId, gameStartDate, location);
    }

    @Override
    public Game savePartialGame(LocalDateTime gameStartDate, String location, Long homeClubId, Long awayClubId) {
        return gameService.savePartialGame(gameStartDate, location, homeClubId, awayClubId);
    }

    @Override
    public Game getGame(Long gameId) {
        return gameService.getGame(gameId);
    }

    @Override
    public List<Game> getGames(List<Long> gameIds) {
        return gameService.getGames(gameIds);
    }
}
