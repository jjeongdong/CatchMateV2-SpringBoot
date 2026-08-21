package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.external.GameFetchPort;
import com.back.catchmate.admin.application.port.out.dto.AdminGameInfo;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminGameFetchAdapter implements GameFetchPort {
    private final GameService gameService;

    @Override
    public AdminGameInfo getGame(Long gameId) {
        GameSummary response = gameService.getGameSummary(gameId);
        if (response == null) return null;
        return new AdminGameInfo(
                response.gameId(),
                response.gameStartDate(),
                response.location(),
                response.homeClubId(),
                response.awayClubId()
        );
    }
}
