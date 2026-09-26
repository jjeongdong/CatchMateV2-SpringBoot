package com.back.catchmate.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.auth.application.dto.command.OAuthLoginCommand;
import com.back.catchmate.auth.application.dto.command.SignUpCommand;
import com.back.catchmate.auth.application.dto.result.OAuthLoginResult;
import com.back.catchmate.auth.application.dto.result.SignUpResult;
import com.back.catchmate.auth.application.dto.result.TokenReissueResult;
import com.back.catchmate.auth.domain.AuthTokenProvider;
import com.back.catchmate.auth.domain.OAuthClient;
import com.back.catchmate.auth.domain.OAuthProfile;
import com.back.catchmate.auth.domain.Provider;
import com.back.catchmate.auth.domain.RefreshTokenRepository;
import com.back.catchmate.auth.domain.event.LoggedOutEvent;
import com.back.catchmate.auth.domain.exception.InvalidRefreshTokenException;
import com.back.catchmate.auth.domain.exception.OAuthProviderException;
import com.back.catchmate.auth.domain.exception.OAuthStateMismatchException;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.user.application.UserCommandService;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import com.back.catchmate.user.application.dto.command.UserCreateCommand;
import com.back.catchmate.user.application.dto.result.UserCreateResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

    private static final Long USER_ID = 7L;
    private static final String REFRESH_TOKEN = "refresh";
    private static final long TTL = 120_000L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final OAuthProfile PROFILE = new OAuthProfile(Provider.KAKAO, "123", "a@b.com", "https://img");

    @Mock
    private AuthTokenProvider tokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private OAuthClientRegistry oauthClientRegistry;

    @Mock
    private OAuthClient oauthClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private UserCommandService userCommandService;

    @Mock
    private ClubQueryApi clubQueryApi;

    @InjectMocks
    private AuthCommandService authCommandService;

    @Nested
    @DisplayName("토큰 재발급")
    class ReissueToken {

        @Test
        @DisplayName("Redis 에 없는 refresh token 이면 InvalidRefreshTokenException")
        void throwsWhenRefreshTokenNotStored() {
            // given
            given(tokenProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
            given(refreshTokenRepository.exists(REFRESH_TOKEN)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authCommandService.reissueToken(REFRESH_TOKEN))
                    .isInstanceOf(InvalidRefreshTokenException.class);
            then(userQueryApi).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("토큰의 role 이 아니라 최신 권한으로 access token 을 발급한다")
        void issuesWithLatestAuthority() {
            // given
            given(tokenProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);
            given(refreshTokenRepository.exists(REFRESH_TOKEN)).willReturn(true);
            given(userQueryApi.getInfo(USER_ID)).willReturn(userInfo("ROLE_ADMIN"));
            given(tokenProvider.createAccessToken(USER_ID, "ROLE_ADMIN")).willReturn("Bearer new");

            // when
            TokenReissueResult result = authCommandService.reissueToken(REFRESH_TOKEN);

            // then
            assertThat(result).isEqualTo(new TokenReissueResult("Bearer new"));
        }
    }

    @Test
    @DisplayName("로그아웃하면 refresh token 을 지우고 LoggedOutEvent 를 발행한다")
    void logoutDeletesTokenAndPublishesEvent() {
        // given
        given(tokenProvider.getUserId(REFRESH_TOKEN)).willReturn(USER_ID);

        // when
        authCommandService.logout(REFRESH_TOKEN);

        // then
        then(refreshTokenRepository).should().delete(REFRESH_TOKEN);
        then(eventPublisher).should().publishEvent(new LoggedOutEvent(USER_ID));
    }

    @Nested
    @DisplayName("OAuth 로그인 완료")
    class CompleteOAuthLogin {

        @Test
        @DisplayName("공급자가 오류를 돌려주면 공급자를 호출하지 않고 OAuthProviderException")
        void throwsWhenProviderReturnsError() {
            // given
            OAuthLoginCommand command =
                    new OAuthLoginCommand(Provider.KAKAO, null, "s", "s", "access_denied", "user denied");

            // when & then
            assertThatThrownBy(() -> authCommandService.completeOAuthLogin(command))
                    .isInstanceOf(OAuthProviderException.class);
            then(oauthClientRegistry).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("code 가 비어 있으면 OAuthProviderException")
        void throwsWhenCodeIsBlank() {
            // given
            OAuthLoginCommand command = new OAuthLoginCommand(Provider.KAKAO, " ", "s", "s", null, null);

            // when & then
            assertThatThrownBy(() -> authCommandService.completeOAuthLogin(command))
                    .isInstanceOf(OAuthProviderException.class);
        }

        @Test
        @DisplayName("state 가 쿠키와 다르면 OAuthStateMismatchException")
        void throwsWhenStateMismatches() {
            // given
            OAuthLoginCommand command = new OAuthLoginCommand(Provider.KAKAO, "code", "s1", "s2", null, null);

            // when & then
            assertThatThrownBy(() -> authCommandService.completeOAuthLogin(command))
                    .isInstanceOf(OAuthStateMismatchException.class);
            then(oauthClientRegistry).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("state 쿠키가 없으면 OAuthStateMismatchException")
        void throwsWhenStateCookieMissing() {
            // given
            OAuthLoginCommand command = new OAuthLoginCommand(Provider.KAKAO, "code", "s1", null, null, null);

            // when & then
            assertThatThrownBy(() -> authCommandService.completeOAuthLogin(command))
                    .isInstanceOf(OAuthStateMismatchException.class);
        }

        @Test
        @DisplayName("가입된 회원이면 토큰을 발급하고 refresh token 을 저장한다")
        void issuesTokensForExistingUser() {
            // given
            givenProviderReturnsProfile();
            given(userQueryApi.findInfoByProviderId("123@kakao")).willReturn(Optional.of(userInfo("ROLE_USER")));
            givenTokensIssued();

            // when
            OAuthLoginResult result = authCommandService.completeOAuthLogin(validCommand());

            // then
            assertThat(result).isEqualTo(new OAuthLoginResult.Existing("Bearer access", REFRESH_TOKEN));
            then(refreshTokenRepository).should().save(REFRESH_TOKEN, USER_ID, TTL);
        }

        @Test
        @DisplayName("가입되지 않은 회원이면 signup token 을 발급한다")
        void issuesSignupTokenForNewUser() {
            // given
            givenProviderReturnsProfile();
            given(userQueryApi.findInfoByProviderId("123@kakao")).willReturn(Optional.empty());
            given(tokenProvider.createSignupToken(PROFILE)).willReturn("signup");

            // when
            OAuthLoginResult result = authCommandService.completeOAuthLogin(validCommand());

            // then
            assertThat(result).isEqualTo(new OAuthLoginResult.NewUser("signup"));
            then(refreshTokenRepository).shouldHaveNoInteractions();
        }

        private OAuthLoginCommand validCommand() {
            return new OAuthLoginCommand(Provider.KAKAO, "code", "s", "s", null, null);
        }

        private void givenProviderReturnsProfile() {
            given(oauthClientRegistry.get(Provider.KAKAO)).willReturn(oauthClient);
            given(oauthClient.exchange("code")).willReturn(PROFILE);
        }
    }

    @Test
    @DisplayName("가입하면 signup token 의 프로필로 회원을 만들고 토큰을 발급한다")
    void signUpCreatesUserAndIssuesTokens() {
        // given
        SignUpCommand command = new SignUpCommand("signup", 'M', "닉네임", LocalDate.of(2000, 1, 1), 3L, "응원");
        given(tokenProvider.parseSignupToken("signup")).willReturn(PROFILE);
        UserCreateCommand expectedCreate = new UserCreateCommand(
                "kakao", "123@kakao", "a@b.com", "닉네임", 'M', LocalDate.of(2000, 1, 1), 3L, "https://img", "응원");
        given(userCommandService.createUser(expectedCreate))
                .willReturn(new UserCreateResult(USER_ID, "ROLE_USER", CREATED_AT));
        givenTokensIssued();

        // when
        SignUpResult result = authCommandService.signUp(command);

        // then
        then(clubQueryApi).should().getInfo(3L);
        then(refreshTokenRepository).should().save(REFRESH_TOKEN, USER_ID, TTL);
        assertThat(result).isEqualTo(new SignUpResult(USER_ID, "Bearer access", CREATED_AT, REFRESH_TOKEN));
    }

    private void givenTokensIssued() {
        given(tokenProvider.createAccessToken(USER_ID, "ROLE_USER")).willReturn("Bearer access");
        given(tokenProvider.createRefreshToken(USER_ID, "ROLE_USER")).willReturn(REFRESH_TOKEN);
        given(tokenProvider.refreshTokenTtlMillis()).willReturn(TTL);
    }

    private static UserInfo userInfo(String authority) {
        return new UserInfo(
                USER_ID,
                "a@b.com",
                "kakao",
                "123@kakao",
                'M',
                "닉네임",
                LocalDate.of(2000, 1, 1),
                null,
                null,
                authority,
                null,
                3L,
                true,
                true,
                true,
                false,
                CREATED_AT,
                CREATED_AT);
    }
}
