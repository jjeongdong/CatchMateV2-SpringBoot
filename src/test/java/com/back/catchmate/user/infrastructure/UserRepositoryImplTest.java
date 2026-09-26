package com.back.catchmate.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.user.domain.User;
import com.back.catchmate.user.domain.exception.UserNotFoundException;
import com.back.catchmate.user.fixture.UserFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @InjectMocks
    private UserRepositoryImpl userRepository;

    @Test
    @DisplayName("유저가 있으면 반환한다")
    void getByIdReturnsUser() {
        // given
        User user = UserFixture.user(1L, 3L);
        given(userJpaRepository.findById(1L)).willReturn(Optional.of(user));

        // when & then
        assertThat(userRepository.getById(1L)).isSameAs(user);
    }

    @Test
    @DisplayName("유저가 없으면 UserNotFoundException 을 던진다")
    void getByIdThrowsWhenMissing() {
        // given
        given(userJpaRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userRepository.getById(99L)).isInstanceOf(UserNotFoundException.class);
    }
}
