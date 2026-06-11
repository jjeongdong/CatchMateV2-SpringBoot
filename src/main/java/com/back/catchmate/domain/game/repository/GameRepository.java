package com.back.catchmate.domain.game.repository;

import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.game.model.Game;

import java.time.LocalDateTime;
import java.util.Optional;

public interface GameRepository {
    Game save(Game game);

    Optional<Game> findByHomeClubAndAwayClubAndGameStartDate(Club homeClub, Club awayClub, LocalDateTime gameStartDate);
}
