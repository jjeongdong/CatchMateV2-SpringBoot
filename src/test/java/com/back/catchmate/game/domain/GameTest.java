package com.back.catchmate.game.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameTest {

    @Test
    @DisplayName("홈·원정 구단, 시작 시각, 장소로 경기를 만든다")
    void createsGame() {
        // given
        LocalDateTime startDate = LocalDateTime.of(2026, 5, 1, 18, 30);

        // when
        Game game = Game.create(1L, 2L, startDate, "잠실");

        // then
        assertThat(game.getId()).isNull();
        assertThat(game.getHomeClubId()).isEqualTo(1L);
        assertThat(game.getAwayClubId()).isEqualTo(2L);
        assertThat(game.getGameStartDate()).isEqualTo(startDate);
        assertThat(game.getLocation()).isEqualTo("잠실");
    }
}
