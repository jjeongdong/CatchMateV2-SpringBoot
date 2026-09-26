package com.back.catchmate.club.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.club.domain.ClubRepository;
import com.back.catchmate.club.domain.exception.ClubNotFoundException;
import com.back.catchmate.club.fixture.ClubFixture;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClubQueryApiTest {

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private ClubQueryApi clubQueryApi;

    @Nested
    @DisplayName("단건 조회")
    class GetInfo {

        @Test
        @DisplayName("구단 정보를 반환한다")
        void returnsInfo() {
            // given
            given(clubRepository.getById(1L)).willReturn(ClubFixture.lgTwins(1L));

            // when
            ClubInfo info = clubQueryApi.getInfo(1L);

            // then
            assertThat(info).isEqualTo(new ClubInfo(1L, "LG 트윈스", "잠실", "서울"));
        }

        @Test
        @DisplayName("구단이 없으면 ClubNotFoundException 을 던진다")
        void throwsWhenMissing() {
            // given
            given(clubRepository.getById(99L)).willThrow(new ClubNotFoundException());

            // when & then
            assertThatThrownBy(() -> clubQueryApi.getInfo(99L)).isInstanceOf(ClubNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("여러 건 조회")
    class GetInfos {

        @Test
        @DisplayName("구단 ID 를 키로 한 맵을 반환한다")
        void returnsInfosById() {
            // given
            given(clubRepository.findAllByIds(List.of(1L, 2L)))
                    .willReturn(List.of(ClubFixture.lgTwins(1L), ClubFixture.doosanBears(2L)));

            // when
            Map<Long, ClubInfo> infosById = clubQueryApi.getInfos(List.of(1L, 2L));

            // then
            assertThat(infosById).containsOnlyKeys(1L, 2L);
            assertThat(infosById.get(2L).name()).isEqualTo("두산 베어스");
        }

        @Test
        @DisplayName("없는 ID 는 결과에서 빠진다")
        void excludesMissingIds() {
            // given
            given(clubRepository.findAllByIds(List.of(1L, 99L))).willReturn(List.of(ClubFixture.lgTwins(1L)));

            // when
            Map<Long, ClubInfo> infosById = clubQueryApi.getInfos(List.of(1L, 99L));

            // then
            assertThat(infosById).containsOnlyKeys(1L);
        }
    }

    @Nested
    @DisplayName("이름으로 조회")
    class FindInfoByName {

        @Test
        @DisplayName("구단이 있으면 정보를 담아 반환한다")
        void returnsInfoWhenPresent() {
            // given
            given(clubRepository.findByName("LG 트윈스")).willReturn(Optional.of(ClubFixture.lgTwins(1L)));

            // when
            Optional<ClubInfo> info = clubQueryApi.findInfoByName("LG 트윈스");

            // then
            assertThat(info).contains(new ClubInfo(1L, "LG 트윈스", "잠실", "서울"));
        }

        @Test
        @DisplayName("구단이 없으면 빈 값을 반환한다")
        void returnsEmptyWhenMissing() {
            // given
            given(clubRepository.findByName("없는 구단")).willReturn(Optional.empty());

            // when
            Optional<ClubInfo> info = clubQueryApi.findInfoByName("없는 구단");

            // then
            assertThat(info).isEmpty();
        }
    }
}
