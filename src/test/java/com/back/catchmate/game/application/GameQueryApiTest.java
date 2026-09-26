package com.back.catchmate.game.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.game.domain.GameRepository;
import com.back.catchmate.game.domain.exception.GameNotFoundException;
import com.back.catchmate.game.fixture.GameFixture;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameQueryApiTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 1, 18, 30);

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameQueryApi gameQueryApi;

    @Nested
    @DisplayName("단건 조회")
    class GetInfo {

        @Test
        @DisplayName("경기 정보를 반환한다")
        void returnsInfo() {
            // given
            given(gameRepository.getById(1L)).willReturn(GameFixture.game(1L, 10L, 20L, START));

            // when
            GameInfo info = gameQueryApi.getInfo(1L);

            // then
            assertThat(info).isEqualTo(new GameInfo(1L, START, "잠실", 10L, 20L));
        }

        @Test
        @DisplayName("경기가 없으면 GameNotFoundException 을 던진다")
        void throwsWhenMissing() {
            // given
            given(gameRepository.getById(99L)).willThrow(new GameNotFoundException());

            // when & then
            assertThatThrownBy(() -> gameQueryApi.getInfo(99L)).isInstanceOf(GameNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("여러 건 조회")
    class GetInfos {

        @Test
        @DisplayName("경기 ID 를 키로 한 맵을 반환한다")
        void returnsInfosById() {
            // given
            given(gameRepository.findAllByIds(List.of(1L, 2L)))
                    .willReturn(List.of(GameFixture.game(1L, 10L, 20L, START), GameFixture.game(2L, 30L, 40L, START)));

            // when
            Map<Long, GameInfo> infosById = gameQueryApi.getInfos(List.of(1L, 2L));

            // then
            assertThat(infosById).containsOnlyKeys(1L, 2L);
            assertThat(infosById.get(2L).homeClubId()).isEqualTo(30L);
        }

        @Test
        @DisplayName("없는 ID 는 결과에서 빠진다")
        void excludesMissingIds() {
            // given
            given(gameRepository.findAllByIds(List.of(1L, 99L)))
                    .willReturn(List.of(GameFixture.game(1L, 10L, 20L, START)));

            // when
            Map<Long, GameInfo> infosById = gameQueryApi.getInfos(List.of(1L, 99L));

            // then
            assertThat(infosById).containsOnlyKeys(1L);
        }
    }

    @Test
    @DisplayName("날짜로 경기 ID 를 조회한다")
    void returnsIdsStartingOn() {
        // given
        LocalDate gameDate = LocalDate.of(2026, 5, 1);
        given(gameRepository.findIdsStartingOn(gameDate)).willReturn(List.of(1L, 2L));

        // when
        List<Long> gameIds = gameQueryApi.getIdsStartingOn(gameDate);

        // then
        assertThat(gameIds).containsExactly(1L, 2L);
    }
}
