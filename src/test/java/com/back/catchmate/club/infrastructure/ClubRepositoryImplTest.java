package com.back.catchmate.club.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.club.domain.Club;
import com.back.catchmate.club.domain.exception.ClubNotFoundException;
import com.back.catchmate.club.fixture.ClubFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClubRepositoryImplTest {

    @Mock
    private ClubJpaRepository clubJpaRepository;

    @InjectMocks
    private ClubRepositoryImpl clubRepository;

    @Nested
    @DisplayName("ID 로 조회")
    class GetById {

        @Test
        @DisplayName("구단이 있으면 반환한다")
        void returnsClub() {
            // given
            Club club = ClubFixture.lgTwins(1L);
            given(clubJpaRepository.findById(1L)).willReturn(Optional.of(club));

            // when & then
            assertThat(clubRepository.getById(1L)).isSameAs(club);
        }

        @Test
        @DisplayName("구단이 없으면 ClubNotFoundException 을 던진다")
        void throwsWhenMissing() {
            // given
            given(clubJpaRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clubRepository.getById(99L)).isInstanceOf(ClubNotFoundException.class);
        }
    }
}
