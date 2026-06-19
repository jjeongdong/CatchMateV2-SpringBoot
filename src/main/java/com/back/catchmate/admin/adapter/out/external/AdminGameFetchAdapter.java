package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.external.GameFetchPort;
import com.back.catchmate.admin.application.port.out.dto.AdminGameInfo;
import com.back.catchmate.game.application.dto.response.GameInternalResponse;
import com.back.catchmate.game.application.port.in.GameInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminGameFetchAdapter implements GameFetchPort {
    private final GameInternalQueryUseCase gameInternalQueryUseCase;

    @Override
    public AdminGameInfo getGame(Long gameId) {
        GameInternalResponse response = gameInternalQueryUseCase.getGame(gameId);
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
