package com.back.catchmate.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.dto.result.AdminUserDetailResult;
import com.back.catchmate.user.application.dto.result.AdminUserResult;
import com.back.catchmate.user.application.dto.result.NicknameAvailabilityResult;
import com.back.catchmate.user.application.dto.result.UserResult;
import com.back.catchmate.user.domain.UserRepository;
import com.back.catchmate.user.fixture.UserFixture;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubQueryApi clubQueryApi;

    @InjectMocks
    private UserQueryService userQueryService;

    @Test
    @DisplayName("유저 조회는 응원 구단을 담아 돌려준다")
    void getUserWithClub() {
        // given
        given(userRepository.getById(1L)).willReturn(UserFixture.user(1L, 3L));
        given(clubQueryApi.getInfo(3L)).willReturn(new ClubInfo(3L, "LG 트윈스", "잠실", "서울"));

        // when
        UserResult result = userQueryService.getUser(1L);

        // then
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.club()).isEqualTo(new UserResult.ClubView(3L, "LG 트윈스", "잠실", "서울"));
    }

    @Test
    @DisplayName("이미 쓰는 닉네임이면 사용할 수 없다고 돌려준다")
    void nicknameTaken() {
        // given
        given(userRepository.existsByNickName("홍길동")).willReturn(true);

        // when
        NicknameAvailabilityResult result = userQueryService.getNicknameAvailability("홍길동");

        // then
        assertThat(result).isEqualTo(new NicknameAvailabilityResult("홍길동", false));
    }

    @Test
    @DisplayName("관리자 회원 상세는 구단 이름을 담는다")
    void getAdminUser() {
        // given
        given(userRepository.getById(1L)).willReturn(UserFixture.user(1L, 3L));
        given(clubQueryApi.getInfo(3L)).willReturn(new ClubInfo(3L, "LG 트윈스", "잠실", "서울"));

        // when
        AdminUserDetailResult result = userQueryService.getAdminUser(1L);

        // then
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.clubName()).isEqualTo("LG 트윈스");
        assertThat(result.role()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("구단이 없는 회원은 구단을 조회하지 않고 구단 이름이 null 이다")
    void getAdminUserWithoutClub() {
        // given
        given(userRepository.getById(1L)).willReturn(UserFixture.user(1L, null));

        // when
        AdminUserDetailResult result = userQueryService.getAdminUser(1L);

        // then
        assertThat(result.clubName()).isNull();
        then(clubQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("관리자 회원 목록은 구단으로 거르고 구단 이름을 한 번에 채운다")
    void getAdminUsersByClub() {
        // given
        given(clubQueryApi.findInfoByName("LG 트윈스")).willReturn(Optional.of(new ClubInfo(3L, "LG 트윈스", "잠실", "서울")));
        given(userRepository.findAllByClubId(3L, 20L, 20))
                .willReturn(List.of(UserFixture.user(1L, 3L), UserFixture.user(2L, null)));
        given(userRepository.countByClubId(3L)).willReturn(22L);
        given(clubQueryApi.getInfos(List.of(3L))).willReturn(Map.of(3L, new ClubInfo(3L, "LG 트윈스", "잠실", "서울")));

        // when
        OffsetPageResult<AdminUserResult> result = userQueryService.getAdminUsers("LG 트윈스", 1, 20);

        // then
        assertThat(result.content()).extracting(AdminUserResult::clubName).containsExactly("LG 트윈스", null);
        assertThat(result.totalElements()).isEqualTo(22L);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("구단 이름이 비어 있으면 전체 회원을 조회한다")
    void getAdminUsersAll() {
        // given
        given(userRepository.findAllByClubId(null, 0L, 20)).willReturn(List.of());
        given(userRepository.countByClubId(null)).willReturn(0L);

        // when
        OffsetPageResult<AdminUserResult> result = userQueryService.getAdminUsers(" ", 0, 20);

        // then
        assertThat(result.content()).isEmpty();
        then(clubQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("없는 구단 이름이면 전체가 아니라 빈 목록이다")
    void getAdminUsersUnknownClubReturnsEmpty() {
        // given
        given(clubQueryApi.findInfoByName("없는 구단")).willReturn(Optional.empty());

        // when
        OffsetPageResult<AdminUserResult> result = userQueryService.getAdminUsers("없는 구단", 0, 20);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        then(userRepository).shouldHaveNoInteractions();
    }
}
