package com.back.catchmate.game.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.game.domain.Game;
import com.back.catchmate.game.domain.exception.GameNotFoundException;
import com.back.catchmate.game.fixture.GameFixture;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameRepositoryImplTest {

    @Mock
    private GameJpaRepository gameJpaRepository;

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @InjectMocks
    private GameRepositoryImpl gameRepository;

    @Nested
    @DisplayName("ID 로 조회")
    class GetById {

        @Test
        @DisplayName("경기가 있으면 반환한다")
        void returnsGame() {
            // given
            Game game = GameFixture.game(1L, 1L, 2L, LocalDateTime.of(2026, 5, 1, 18, 30));
            given(gameJpaRepository.findById(1L)).willReturn(Optional.of(game));

            // when & then
            assertThat(gameRepository.getById(1L)).isSameAs(game);
        }

        @Test
        @DisplayName("경기가 없으면 GameNotFoundException 을 던진다")
        void throwsWhenMissing() {
            // given
            given(gameJpaRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> gameRepository.getById(99L)).isInstanceOf(GameNotFoundException.class);
        }
    }
}
