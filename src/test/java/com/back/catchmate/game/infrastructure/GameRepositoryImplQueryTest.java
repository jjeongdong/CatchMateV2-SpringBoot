package com.back.catchmate.game.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.game.domain.Game;
import com.back.catchmate.game.domain.GameSearchCondition;
import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// application.yml 의 기본 프로필 dev 는 운영 RDS 를 가리킨다. create-drop 이 운영 DB 에 닿지 않도록
// 존재하지 않는 test 프로필로 dev 설정을 끄고, datasource 는 컨테이너(@ServiceConnection)에서만 받는다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QuerydslConfig.class, JpaAuditingConfig.class, GameRepositoryImpl.class})
@Testcontainers(disabledWithoutDocker = true)
class GameRepositoryImplQueryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    private static final LocalDate GAME_DATE = LocalDate.of(2026, 5, 1);

    @Autowired
    private GameRepositoryImpl gameRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Game persist(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate) {
        return entityManager.persist(Game.create(homeClubId, awayClubId, gameStartDate, "잠실"));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("조건 조회")
    class FindAllByCondition {

        @Test
        @DisplayName("날짜 필터는 당일 00:00 은 포함하고 다음날 00:00 은 제외한다")
        void filtersByDateWithHalfOpenRange() {
            // given
            persist(1L, 2L, GAME_DATE.minusDays(1).atTime(23, 59));
            Game midnight = persist(1L, 2L, GAME_DATE.atStartOfDay());
            Game lateNight = persist(1L, 2L, GAME_DATE.atTime(23, 59));
            persist(1L, 2L, GAME_DATE.plusDays(1).atStartOfDay());
            flushAndClear();

            // when
            List<Game> games = gameRepository.findAllByCondition(new GameSearchCondition(GAME_DATE, null));

            // then
            assertThat(games).extracting(Game::getId).containsExactly(midnight.getId(), lateNight.getId());
        }

        @Test
        @DisplayName("구단 필터는 홈·원정 어느 쪽이든 매칭한다")
        void matchesClubOnEitherSide() {
            // given
            Game home = persist(1L, 2L, GAME_DATE.atTime(14, 0));
            Game away = persist(3L, 1L, GAME_DATE.atTime(15, 0));
            persist(2L, 3L, GAME_DATE.atTime(16, 0));
            flushAndClear();

            // when
            List<Game> games = gameRepository.findAllByCondition(new GameSearchCondition(null, 1L));

            // then
            assertThat(games).extracting(Game::getId).containsExactly(home.getId(), away.getId());
        }

        @Test
        @DisplayName("조건이 없으면 시작 시각, 같으면 id 오름차순으로 전체를 반환한다")
        void returnsAllSortedByStartThenId() {
            // given
            Game evening = persist(1L, 2L, GAME_DATE.atTime(18, 30));
            Game firstAfternoon = persist(3L, 4L, GAME_DATE.atTime(14, 0));
            Game secondAfternoon = persist(5L, 6L, GAME_DATE.atTime(14, 0));
            flushAndClear();

            // when
            List<Game> games = gameRepository.findAllByCondition(new GameSearchCondition(null, null));

            // then
            assertThat(games)
                    .extracting(Game::getId)
                    .containsExactly(firstAfternoon.getId(), secondAfternoon.getId(), evening.getId());
        }
    }

    @Test
    @DisplayName("날짜로 경기 ID 를 조회할 때 당일 00:00 은 포함하고 다음날 00:00 은 제외한다")
    void findsIdsStartingOnWithHalfOpenRange() {
        // given
        persist(1L, 2L, GAME_DATE.minusDays(1).atTime(23, 59));
        Game midnight = persist(1L, 2L, GAME_DATE.atStartOfDay());
        persist(1L, 2L, GAME_DATE.plusDays(1).atStartOfDay());
        flushAndClear();

        // when
        List<Long> gameIds = gameRepository.findIdsStartingOn(GAME_DATE);

        // then
        assertThat(gameIds).containsExactly(midnight.getId());
    }
}
