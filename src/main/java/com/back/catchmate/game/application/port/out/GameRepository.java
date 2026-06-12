package com.back.catchmate.game.application.port.out;

import com.back.catchmate.game.domain.model.Game;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GameRepository {
    Game save(Game game);

    Optional<Game> findById(Long id);

    Optional<Game> findByHomeClubIdAndAwayClubIdAndGameStartDate(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate);

    List<Game> findAllByIds(List<Long> ids);
}
