package com.back.catchmate.domain.game.service;

import com.back.catchmate.domain.game.model.Game;
import com.back.catchmate.domain.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;

    public Game createGame(Game game) {
        return gameRepository.save(game);
    }
}
