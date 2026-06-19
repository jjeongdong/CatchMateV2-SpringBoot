package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.external.GameFetchPort;
import com.back.catchmate.board.application.port.out.dto.BoardGameInfo;
import com.back.catchmate.game.application.dto.response.GameInternalResponse;
import com.back.catchmate.game.application.port.in.GameInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BoardGameFetchAdapter implements GameFetchPort {
    private final GameInternalQueryUseCase gameInternalQueryUseCase;

    @Override
    public BoardGameInfo getGame(Long gameId) {
        GameInternalResponse response = gameInternalQueryUseCase.getGame(gameId);
        return toBoardGameInfo(response);
    }

    @Override
    public List<BoardGameInfo> getGames(List<Long> gameIds) {
        return gameInternalQueryUseCase.getGames(gameIds).stream()
                .map(this::toBoardGameInfo)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findGameIdsByDate(LocalDate gameDate) {
        return gameInternalQueryUseCase.findIdsByGameStartDateOn(gameDate);
    }

    private BoardGameInfo toBoardGameInfo(GameInternalResponse response) {
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
