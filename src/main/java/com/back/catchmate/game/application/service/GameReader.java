package com.back.catchmate.game.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.game.application.port.out.persistence.GameRepository;
import com.back.catchmate.game.domain.model.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GameReader {
    private final GameRepository gameRepository;

    public Game getGame(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new BaseException(ErrorCode.GAME_NOT_FOUND));
    }

    public List<Game> getGames(List<Long> gameIds) {
        return gameRepository.findAllByIds(gameIds);
    }

    public List<Long> findIdsByGameStartDateOn(LocalDate gameDate) {
        return gameRepository.findIdsByGameStartDateOn(gameDate);
    }
}
