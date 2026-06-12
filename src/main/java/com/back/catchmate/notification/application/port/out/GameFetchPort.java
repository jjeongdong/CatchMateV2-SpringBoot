package com.back.catchmate.notification.application.port.out;

import com.back.catchmate.game.domain.model.Game;

import java.util.List;

public interface GameFetchPort {
    Game getGame(Long gameId);
    List<Game> getGames(List<Long> gameIds);
}
