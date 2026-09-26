package com.back.catchmate.game.service;

import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.game.dto.request.GameSearchCondition;
import com.back.catchmate.game.dto.response.GameResponse;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.game.entity.Game;
import com.back.catchmate.game.repository.GameRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 경기 조회 서비스. 현재는 조회 전용이라 클래스 레벨 {@code readOnly = true} 하나로 끝난다.
 *
 * <p>⚠️ 이후 쓰기 메서드(save/delete 등)를 추가하면 그 메서드에 {@code @Transactional}(readOnly 없음)을
 * 반드시 메서드 레벨로 재선언해야 한다. readOnly 트랜잭션은 Hibernate FlushMode 가 MANUAL 이라
 * 재선언을 빠뜨리면 예외 없이 저장이 조용히 무시된다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;

    private final ClubQueryApi clubQueryApi;

    public List<GameResponse> getGameList(LocalDate gameDate, Long clubId) {
        List<Game> games = gameRepository.findAllByCondition(new GameSearchCondition(gameDate, clubId));
        if (games.isEmpty()) {
            return List.of();
        }

        Map<Long, ClubInfo> clubMap = loadClubs(games);
        return games.stream()
                .map(game ->
                        GameResponse.of(game, clubMap.get(game.getHomeClubId()), clubMap.get(game.getAwayClubId())))
                .toList();
    }

    // 다른 컨텍스트용
    public GameSummary getGameSummary(Long gameId) {
        return toSummary(getGameOrThrow(gameId));
    }

    // 다른 컨텍스트용
    public List<GameSummary> getGameSummaries(List<Long> gameIds) {
        if (gameIds == null || gameIds.isEmpty()) {
            return List.of();
        }

        return gameRepository.findAllById(gameIds).stream().map(this::toSummary).collect(Collectors.toList());
    }

    /**
     * 해당 날짜(00:00 ~ 익일 00:00)에 시작하는 경기 ID 목록.
     * 다른 컨텍스트용 — 호출자가 game.gameStartDate 컬럼에 의존하지 않도록 식별자만 반환한다.
     * (List&lt;Long&gt; 반환이라 Summary 표식을 붙일 대상이 없어 이름을 그대로 둔다.)
     */
    public List<Long> findIdsByGameStartDateOn(LocalDate gameDate) {
        LocalDateTime start = gameDate.atStartOfDay();
        LocalDateTime end = gameDate.plusDays(1).atStartOfDay();
        return gameRepository.findIdsByGameStartDateBetween(start, end);
    }

    private Game getGameOrThrow(Long gameId) {
        return gameRepository.findById(gameId).orElseThrow(() -> new BaseException(ErrorCode.GAME_NOT_FOUND));
    }

    private Map<Long, ClubInfo> loadClubs(List<Game> games) {
        List<Long> clubIds = games.stream()
                .flatMap(game -> Stream.of(game.getHomeClubId(), game.getAwayClubId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (clubIds.isEmpty()) {
            return Map.of();
        }

        return clubQueryApi.getInfos(clubIds);
    }

    private GameSummary toSummary(Game game) {
        return new GameSummary(
                game.getId(), game.getGameStartDate(), game.getLocation(), game.getHomeClubId(), game.getAwayClubId());
    }
}
