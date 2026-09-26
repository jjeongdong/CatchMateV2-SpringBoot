package com.back.catchmate.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.user.application.dto.api.UserInfo;
import com.back.catchmate.user.domain.BlockRepository;
import com.back.catchmate.user.domain.UserRepository;
import com.back.catchmate.user.domain.exception.UserNotFoundException;
import com.back.catchmate.user.fixture.UserFixture;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserQueryApiTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlockRepository blockRepository;

    @InjectMocks
    private UserQueryApi userQueryApi;

    @Test
    @DisplayName("유저 하나를 UserInfo 로 조회한다")
    void getInfo() {
        // given
        given(userRepository.getById(1L)).willReturn(UserFixture.user(1L, 3L));

        // when
        UserInfo info = userQueryApi.getInfo(1L);

        // then
        assertThat(info.userId()).isEqualTo(1L);
        assertThat(info.clubId()).isEqualTo(3L);
        assertThat(info.authority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("없는 유저는 UserNotFoundException 이 그대로 전파된다")
    void getInfoPropagatesNotFound() {
        // given
        given(userRepository.getById(99L)).willThrow(new UserNotFoundException());

        // when & then
        assertThatThrownBy(() -> userQueryApi.getInfo(99L)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("여러 유저를 ID 맵으로 조회하고 없는 ID 는 빠진다")
    void getInfosAsMap() {
        // given
        given(userRepository.findAllByIds(List.of(1L, 2L, 99L)))
                .willReturn(List.of(UserFixture.user(1L, 3L), UserFixture.user(2L, 3L)));

        // when
        Map<Long, UserInfo> infoById = userQueryApi.getInfos(List.of(1L, 2L, 99L));

        // then
        assertThat(infoById).containsOnlyKeys(1L, 2L);
    }

    @Test
    @DisplayName("isBlocked 는 첫 인자가 차단한 사람, 둘째 인자가 차단된 사람이다")
    void isBlockedArgumentOrder() {
        // given
        given(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).willReturn(true);

        // when & then
        assertThat(userQueryApi.isBlocked(1L, 2L)).isTrue();
    }
}
