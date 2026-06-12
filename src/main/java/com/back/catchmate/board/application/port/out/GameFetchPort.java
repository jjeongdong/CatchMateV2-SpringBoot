package com.back.catchmate.board.application.port.out;

import com.back.catchmate.game.domain.model.Game;

import java.time.LocalDateTime;
import java.util.List;

public interface GameFetchPort {
    Game findOrCreateGame(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate, String location);
    Game savePartialGame(LocalDateTime gameStartDate, String location, Long homeClubId, Long awayClubId);
    Game getGame(Long gameId);
    List<Game> getGames(List<Long> gameIds);
}
