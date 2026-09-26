package com.back.catchmate.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.user.application.dto.result.NicknameAvailabilityResult;
import com.back.catchmate.user.application.dto.result.UserResult;
import com.back.catchmate.user.domain.UserRepository;
import com.back.catchmate.user.fixture.UserFixture;
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
}
