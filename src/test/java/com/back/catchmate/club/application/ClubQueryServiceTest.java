package com.back.catchmate.club.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.club.application.dto.result.ClubResult;
import com.back.catchmate.club.domain.ClubRepository;
import com.back.catchmate.club.fixture.ClubFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClubQueryServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private ClubQueryService clubQueryService;

    @Test
    @DisplayName("전체 구단을 목록 응답으로 변환한다")
    void returnsAllClubs() {
        // given
        given(clubRepository.findAll()).willReturn(List.of(ClubFixture.lgTwins(1L), ClubFixture.doosanBears(2L)));

        // when
        List<ClubResult> clubs = clubQueryService.getClubs();

        // then
        assertThat(clubs)
                .containsExactly(new ClubResult(1L, "LG 트윈스", "잠실", "서울"), new ClubResult(2L, "두산 베어스", "잠실", "서울"));
    }
}
