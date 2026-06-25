package com.back.catchmate.game.application.service;

import com.back.catchmate.game.application.dto.GameClubInfo;
import com.back.catchmate.game.application.dto.response.GameResponse;
import com.back.catchmate.game.application.port.in.GameClientQueryUseCase;
import com.back.catchmate.game.application.port.out.external.ClubFetchPort;
import com.back.catchmate.game.domain.dto.GameSearchCondition;
import com.back.catchmate.game.domain.model.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameClientQueryService implements GameClientQueryUseCase {
    private final GameReader gameReader;

    private final ClubFetchPort clubFetchPort;

    @Override
    public List<GameResponse> getGameList(LocalDate gameDate, Long clubId) {
        List<Game> games = gameReader.getGameList(new GameSearchCondition(gameDate, clubId));
        if (games.isEmpty()) {
            return List.of();
        }

        Map<Long, GameClubInfo> clubMap = loadClubs(games);
        return games.stream()
                .map(game -> GameResponse.of(game, clubMap.get(game.getHomeClubId()), clubMap.get(game.getAwayClubId())))
                .toList();
    }

    private Map<Long, GameClubInfo> loadClubs(List<Game> games) {
        List<Long> clubIds = games.stream()
                .flatMap(game -> Stream.of(game.getHomeClubId(), game.getAwayClubId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (clubIds.isEmpty()) {
            return Map.of();
        }

        return clubFetchPort.getClubs(clubIds).stream()
                .collect(Collectors.toMap(GameClubInfo::clubId, Function.identity()));
    }
}
