package com.back.catchmate.domain.game.repository;

import com.back.catchmate.domain.game.model.Game;

import java.util.Optional;

public interface GameRepository {
    Game save(Game game);
    Optional<Game> findById(Long id);
}
