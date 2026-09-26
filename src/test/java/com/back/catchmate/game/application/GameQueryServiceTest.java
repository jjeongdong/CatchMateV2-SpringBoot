package com.back.catchmate.game.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.application.dto.result.GameResult;
import com.back.catchmate.game.domain.GameRepository;
import com.back.catchmate.game.domain.GameSearchCondition;
import com.back.catchmate.game.fixture.GameFixture;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameQueryServiceTest {

    private static final LocalDate GAME_DATE = LocalDate.of(2026, 5, 1);
    private static final LocalDateTime START = GAME_DATE.atTime(18, 30);
    private static final ClubInfo LG = new ClubInfo(1L, "LG 트윈스", "잠실", "서울");
    private static final ClubInfo DOOSAN = new ClubInfo(2L, "두산 베어스", "잠실", "서울");

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ClubQueryApi clubQueryApi;

    @InjectMocks
    private GameQueryService gameQueryService;

    @Test
    @DisplayName("경기마다 홈·원정 구단 정보를 채워 반환한다")
    void returnsGamesWithClubs() {
        // given
        given(gameRepository.findAllByCondition(new GameSearchCondition(GAME_DATE, null)))
                .willReturn(List.of(GameFixture.game(100L, 1L, 2L, START)));
        given(clubQueryApi.getInfos(List.of(1L, 2L))).willReturn(Map.of(1L, LG, 2L, DOOSAN));

        // when
        List<GameResult> games = gameQueryService.getGames(GAME_DATE, null);

        // then
        assertThat(games)
                .containsExactly(new GameResult(
                        100L,
                        START,
                        "잠실",
                        new GameResult.ClubView(1L, "LG 트윈스", "잠실", "서울"),
                        new GameResult.ClubView(2L, "두산 베어스", "잠실", "서울")));
    }

    @Test
    @DisplayName("구단 ID 가 비었거나 구단 정보가 없으면 해당 구단만 null 로 둔다")
    void returnsNullClubViewWhenClubMissing() {
        // given
        given(gameRepository.findAllByCondition(new GameSearchCondition(null, null)))
                .willReturn(List.of(GameFixture.game(100L, 1L, null, START), GameFixture.game(101L, 99L, 1L, START)));
        given(clubQueryApi.getInfos(List.of(1L, 99L))).willReturn(Map.of(1L, LG));

        // when
        List<GameResult> games = gameQueryService.getGames(null, null);

        // then
        assertThat(games.get(0).awayClub()).isNull();
        assertThat(games.get(1).homeClub()).isNull();
        assertThat(games.get(1).awayClub().name()).isEqualTo("LG 트윈스");
    }

    @Test
    @DisplayName("경기가 없으면 구단을 조회하지 않고 빈 목록을 반환한다")
    void returnsEmptyWithoutClubLookup() {
        // given
        given(gameRepository.findAllByCondition(new GameSearchCondition(GAME_DATE, 1L)))
                .willReturn(List.of());

        // when
        List<GameResult> games = gameQueryService.getGames(GAME_DATE, 1L);

        // then
        assertThat(games).isEmpty();
        then(clubQueryApi).shouldHaveNoInteractions();
    }
}
