package com.back.catchmate.game.application;

import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.application.dto.result.GameResult;
import com.back.catchmate.game.domain.Game;
import com.back.catchmate.game.domain.GameRepository;
import com.back.catchmate.game.domain.GameSearchCondition;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameQueryService {
    private final GameRepository gameRepository;
    private final ClubQueryApi clubQueryApi;

    @Transactional(readOnly = true)
    public List<GameResult> getGames(LocalDate gameDate, Long clubId) {
        List<Game> games = gameRepository.findAllByCondition(new GameSearchCondition(gameDate, clubId));
        if (games.isEmpty()) {
            return List.of();
        }

        Map<Long, ClubInfo> clubsById = clubQueryApi.getInfos(collectClubIds(games));
        // 구단 ID 가 null 인 경기가 있고, 불변 Map 은 get(null) 에서 NPE 를 던지므로 ID 가 있을 때만 조회한다.
        return games.stream()
                .map(game -> GameResult.of(
                        game,
                        game.getHomeClubId() != null ? clubsById.get(game.getHomeClubId()) : null,
                        game.getAwayClubId() != null ? clubsById.get(game.getAwayClubId()) : null))
                .toList();
    }

    private List<Long> collectClubIds(List<Game> games) {
        return games.stream()
                .flatMap(game -> Stream.of(game.getHomeClubId(), game.getAwayClubId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
