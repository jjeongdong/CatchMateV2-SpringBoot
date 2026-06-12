package com.back.catchmate.admin.application.port.out;

import com.back.catchmate.game.domain.model.Game;

public interface GameFetchPort {
    Game getGame(Long gameId);
}
