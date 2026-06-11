package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.GameFetchPort;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.game.domain.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BoardGameFetchAdapter implements GameFetchPort {
    private final GameService gameService;

    @Override
    public Game findOrCreateGame(Club homeClub, Club awayClub, LocalDateTime gameStartDate, String location) {
        return gameService.findOrCreateGame(homeClub, awayClub, gameStartDate, location);
    }

    @Override
    public Game savePartialGame(LocalDateTime gameStartDate, String location, Club homeClub, Club awayClub) {
        return gameService.savePartialGame(gameStartDate, location, homeClub, awayClub);
    }
}
