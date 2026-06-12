package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.GameFetchPort;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.game.domain.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminGameFetchAdapter implements GameFetchPort {
    private final GameService gameService;

    @Override
    public Game getGame(Long gameId) {
        return gameService.getGame(gameId);
    }
}
