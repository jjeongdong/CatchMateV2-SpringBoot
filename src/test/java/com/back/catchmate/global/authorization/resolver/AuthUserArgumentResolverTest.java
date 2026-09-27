package com.back.catchmate.global.authorization.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.global.error.UnauthorizedException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthUserArgumentResolverTest {

    private final AuthUserArgumentResolver resolver = new AuthUserArgumentResolver();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증된 사용자의 principal 을 userId 로 돌려준다")
    void resolvesUserId() {
        // given
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("7", null, List.of()));

        // when & then
        assertThat(resolver.resolveArgument(null, null, null, null)).isEqualTo(7L);
    }

    @Test
    @DisplayName("로그인하지 않았거나 principal 이 숫자가 아니면 UnauthorizedException")
    void rejectsAnonymousOrMalformed() {
        // given — 익명 사용자
        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken(
                        "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        // when & then
        assertThatThrownBy(() -> resolver.resolveArgument(null, null, null, null))
                .isInstanceOf(UnauthorizedException.class);

        // given — 숫자가 아닌 principal
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("abc", null, List.of()));

        // when & then
        assertThatThrownBy(() -> resolver.resolveArgument(null, null, null, null))
                .isInstanceOf(UnauthorizedException.class);
    }
}
