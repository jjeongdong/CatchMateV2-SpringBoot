package com.back.catchmate.game.application.service;

import com.back.catchmate.game.application.dto.response.GameInternalResponse;
import com.back.catchmate.game.application.port.in.GameInternalQueryUseCase;
import com.back.catchmate.game.domain.model.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameInternalQueryService implements GameInternalQueryUseCase {
    private final GameReader gameReader;

    @Override
    public GameInternalResponse getGame(Long gameId) {
        Game game = gameReader.getGame(gameId);
        return toInternalResponse(game);
    }

    @Override
    public List<GameInternalResponse> getGames(List<Long> gameIds) {
        return gameReader.getGames(gameIds).stream()
                .map(this::toInternalResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findIdsByGameStartDateOn(LocalDate gameDate) {
        return gameReader.findIdsByGameStartDateOn(gameDate);
    }

    private GameInternalResponse toInternalResponse(Game game) {
        return new GameInternalResponse(
                game.getId(),
                game.getGameStartDate(),
                game.getLocation(),
                game.getHomeClubId(),
                game.getAwayClubId()
        );
    }
}
