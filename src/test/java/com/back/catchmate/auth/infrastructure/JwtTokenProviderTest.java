package com.back.catchmate.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.auth.domain.OAuthProfile;
import com.back.catchmate.auth.domain.Provider;
import com.back.catchmate.auth.domain.exception.InvalidSignupTokenException;
import com.back.catchmate.auth.domain.exception.InvalidTokenException;
import com.back.catchmate.global.config.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes!!";

    private final JwtTokenProvider provider =
            new JwtTokenProvider(SECRET, 60_000L, 120_000L, 30_000L, "AccessToken", "RefreshToken");

    @Nested
    @DisplayName("access·refresh 토큰")
    class AuthTokens {

        @Test
        @DisplayName("access 토큰은 Bearer 접두로 발급되고 검증하면 userId·role 을 돌려준다")
        void verifiesAccessToken() {
            // given
            String accessToken = provider.createAccessToken(7L, "ROLE_USER");

            // when
            AuthenticatedUser user = provider.verify(accessToken);

            // then
            assertThat(accessToken).startsWith("Bearer ");
            assertThat(user).isEqualTo(new AuthenticatedUser(7L, "ROLE_USER"));
        }

        @Test
        @DisplayName("refresh 토큰은 접두 없이 발급되고 userId 를 꺼낼 수 있다")
        void readsUserIdFromRefreshToken() {
            // given
            String refreshToken = provider.createRefreshToken(7L, "ROLE_USER");

            // when & then
            assertThat(refreshToken).doesNotStartWith("Bearer ");
            assertThat(provider.getUserId(refreshToken)).isEqualTo(7L);
        }

        @Test
        @DisplayName("토큰이 없으면 InvalidTokenException")
        void throwsWhenTokenIsNull() {
            // when & then
            assertThatThrownBy(() -> provider.getUserId(null)).isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰이면 InvalidTokenException")
        void throwsWhenSignedWithOtherKey() {
            // given
            JwtTokenProvider other = new JwtTokenProvider(
                    "another-secret-key-must-be-at-least-32-bytes", 60_000L, 120_000L, 30_000L, "A", "R");
            String foreignToken = other.createRefreshToken(7L, "ROLE_USER");

            // when & then
            assertThatThrownBy(() -> provider.verify(foreignToken)).isInstanceOf(InvalidTokenException.class);
        }
    }

    @Nested
    @DisplayName("signup 토큰")
    class SignupTokens {

        @Test
        @DisplayName("발급한 signup 토큰을 파싱하면 같은 프로필이 나온다")
        void roundTrip() {
            // given
            OAuthProfile profile = new OAuthProfile(Provider.GOOGLE, "sub-1", "a@b.com", "https://img");

            // when
            OAuthProfile parsed = provider.parseSignupToken(provider.createSignupToken(profile));

            // then
            assertThat(parsed).isEqualTo(profile);
        }

        @Test
        @DisplayName("signup 토큰이 아닌 토큰이면 InvalidSignupTokenException")
        void throwsWhenSubjectIsNotSignup() {
            // given
            String refreshToken = provider.createRefreshToken(7L, "ROLE_USER");

            // when & then
            assertThatThrownBy(() -> provider.parseSignupToken(refreshToken))
                    .isInstanceOf(InvalidSignupTokenException.class);
        }

        @Test
        @DisplayName("형식이 깨진 토큰이면 InvalidSignupTokenException")
        void throwsWhenMalformed() {
            // when & then
            assertThatThrownBy(() -> provider.parseSignupToken("not-a-jwt"))
                    .isInstanceOf(InvalidSignupTokenException.class);
        }
    }
}
