package com.back.catchmate.enroll.adapter.out.external;

import com.back.catchmate.enroll.application.port.out.external.GameFetchPort;
import com.back.catchmate.enroll.application.port.out.dto.EnrollGameInfo;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EnrollGameFetchAdapter implements GameFetchPort {
    private final GameService gameService;

    @Override
    public List<EnrollGameInfo> getGames(List<Long> gameIds) {
        List<GameSummary> responses = gameService.getGameSummaries(gameIds);
        return responses.stream()
                .map(response -> new EnrollGameInfo(
                        response.gameId(),
                        response.gameStartDate(),
                        response.homeClubId(),
                        response.awayClubId(),
                        response.location()
                ))
                .collect(Collectors.toList());
    }
}
