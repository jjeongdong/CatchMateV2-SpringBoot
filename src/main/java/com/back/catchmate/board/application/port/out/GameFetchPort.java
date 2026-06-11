package com.back.catchmate.board.application.port.out;

import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.game.domain.model.Game;

import java.time.LocalDateTime;

public interface GameFetchPort {
    Game findOrCreateGame(Club homeClub, Club awayClub, LocalDateTime gameStartDate, String location);
    Game savePartialGame(LocalDateTime gameStartDate, String location, Club homeClub, Club awayClub);
}
