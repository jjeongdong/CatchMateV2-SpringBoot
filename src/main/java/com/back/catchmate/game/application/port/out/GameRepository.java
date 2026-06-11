package com.back.catchmate.game.application.port.out;

import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.game.domain.model.Game;

import java.time.LocalDateTime;
import java.util.Optional;

public interface GameRepository {
    Game save(Game game);

    Optional<Game> findByHomeClubAndAwayClubAndGameStartDate(Club homeClub, Club awayClub, LocalDateTime gameStartDate);
}
