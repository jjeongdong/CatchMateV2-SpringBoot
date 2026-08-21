package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.external.GameFetchPort;
import com.back.catchmate.board.application.port.out.dto.BoardGameInfo;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BoardGameFetchAdapter implements GameFetchPort {
    private final GameService gameService;

    @Override
    public BoardGameInfo getGame(Long gameId) {
        GameSummary response = gameService.getGameSummary(gameId);
        return toBoardGameInfo(response);
    }

    @Override
    public List<BoardGameInfo> getGames(List<Long> gameIds) {
        return gameService.getGameSummaries(gameIds).stream()
                .map(this::toBoardGameInfo)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findGameIdsByDate(LocalDate gameDate) {
        return gameService.findIdsByGameStartDateOn(gameDate);
    }

    private BoardGameInfo toBoardGameInfo(GameSummary response) {
        if (response == null) return null;
        return new BoardGameInfo(
                response.gameId(),
                response.gameStartDate(),
                response.location(),
                response.homeClubId(),
                response.awayClubId()
        );
    }
}
