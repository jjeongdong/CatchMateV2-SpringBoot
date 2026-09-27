package com.back.catchmate.auth.application;

import com.back.catchmate.auth.application.dto.command.OAuthLoginCommand;
import com.back.catchmate.auth.application.dto.command.SignUpCommand;
import com.back.catchmate.auth.application.dto.result.OAuthAuthorizeResult;
import com.back.catchmate.auth.application.dto.result.OAuthLoginResult;
import com.back.catchmate.auth.application.dto.result.SignUpResult;
import com.back.catchmate.auth.application.dto.result.TokenReissueResult;
import com.back.catchmate.auth.domain.AuthTokenProvider;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// 클래스 레벨에 @Transactional 을 붙이지 말 것.
// completeOAuthLogin 은 OAuth 공급자(카카오·구글)로 토큰 교환 + 사용자 조회 HTTP 를 순차로 호출한다.
// 여기에 트랜잭션이 걸리면 read timeout(5초) 동안 DB 커넥션을 붙잡은 채 외부 응답을 기다리게 되고,
// 인스턴스당 pool=15 이므로 로그인 트래픽이 몰리면 풀이 말라 서비스 전체가 멈춘다.
// 트랜잭션은 DB 를 실제로 건드리는 메서드에만 선언한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCommandService {
    private final AuthTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthClientRegistry oauthClientRegistry;
    private final ApplicationEventPublisher eventPublisher;

    private final UserQueryApi userQueryApi;
    private final UserCommandService userCommandService;
    private final ClubQueryApi clubQueryApi;

    @Transactional(readOnly = true)
    public TokenReissueResult reissueToken(String refreshToken) {
        Long userId = tokenProvider.getUserId(refreshToken);
        if (!refreshTokenRepository.exists(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }
        // refresh token 의 role 클레임을 믿지 않고 최신 권한을 다시 조회한다.
        // (권한이 강등된 사용자가 옛 refresh token 으로 옛 권한을 재발급받는 것을 막는다.)
        UserInfo user = userQueryApi.getInfo(userId);
        return new TokenReissueResult(tokenProvider.createAccessToken(user.userId(), user.authority()));
    }

    // AFTER_COMMIT 리스너(FCM 토큰 제거)가 실행되려면 트랜잭션 안에서 발행해야 한다.
    @Transactional
    public void logout(String refreshToken) {
        Long userId = tokenProvider.getUserId(refreshToken);
        refreshTokenRepository.delete(refreshToken);
        eventPublisher.publishEvent(new LoggedOutEvent(userId));
    }

    // DB 를 쓰지 않으므로 트랜잭션을 열지 않는다 (열면 커넥션만 점유한다).
    public OAuthAuthorizeResult startOAuthLogin(Provider provider) {
        String state = UUID.randomUUID().toString();
        String url = oauthClientRegistry.get(provider).buildAuthorizeUrl(state);
        return new OAuthAuthorizeResult(url, state);
    }

    // 트랜잭션을 열지 않는다 — 클래스 상단 주석 참고.
    public OAuthLoginResult completeOAuthLogin(OAuthLoginCommand command) {
        validateCallback(command);

        OAuthProfile profile = oauthClientRegistry.get(command.provider()).exchange(command.code());
        return userQueryApi
                .findInfoByProviderId(profile.providerIdWithProvider())
                .<OAuthLoginResult>map(user -> {
                    Tokens tokens = issueTokens(user.userId(), user.authority());
                    return new OAuthLoginResult.Existing(tokens.accessToken(), tokens.refreshToken());
                })
                .orElseGet(() -> new OAuthLoginResult.NewUser(tokenProvider.createSignupToken(profile)));
    }

    @Transactional
    public SignUpResult signUp(SignUpCommand command) {
        OAuthProfile profile = tokenProvider.parseSignupToken(command.signupToken());
        // 존재하지 않으면 ClubQueryApi 가 ClubNotFoundException 을 던진다
        clubQueryApi.getInfo(command.favoriteClubId());

        // 새 userId 로 곧바로 토큰을 발급해야 해서 user 생성만은 이벤트가 아닌 동기 호출이다.
        // 아키텍처 테스트의 BoundedContexts.SYNC_COMMAND_ALLOWLIST 에 이 클래스가 등록돼 있다.
        UserCreateResult user = userCommandService.createUser(toUserCreateCommand(command, profile));
        Tokens tokens = issueTokens(user.userId(), user.authority());
        return new SignUpResult(user.userId(), tokens.accessToken(), user.createdAt(), tokens.refreshToken());
    }

    // 엔티티가 없는 BC 라 콜백 검증을 서비스에 둔다.
    private void validateCallback(OAuthLoginCommand command) {
        if (command.error() != null) {
            log.warn(
                    "OAuth 공급자 오류 응답: provider={}, error={}, description={}",
                    command.provider(),
                    command.error(),
                    command.errorDescription());
            throw new OAuthProviderException();
        }
        if (!StringUtils.hasText(command.code()) || !StringUtils.hasText(command.state())) {
            log.warn(
                    "OAuth 콜백에 code/state 누락: provider={}, code={}, state={}",
                    command.provider(),
                    command.code(),
                    command.state());
            throw new OAuthProviderException();
        }
        if (!command.state().equals(command.stateFromCookie())) {
            throw new OAuthStateMismatchException();
        }
    }

    private Tokens issueTokens(Long userId, String role) {
        String accessToken = tokenProvider.createAccessToken(userId, role);
        String refreshToken = tokenProvider.createRefreshToken(userId, role);
        refreshTokenRepository.save(refreshToken, userId, tokenProvider.refreshTokenTtlMillis());
        return new Tokens(accessToken, refreshToken);
    }

    // user 의 Command 는 경계 예외가 허용된 이 클래스 안에서만 만든다 (SignUpCommand 에 두면 그 DTO 가 경계를 넘는다).
    private UserCreateCommand toUserCreateCommand(SignUpCommand command, OAuthProfile profile) {
        return new UserCreateCommand(
                profile.provider().value(),
                profile.providerIdWithProvider(),
                profile.email(),
                command.nickName(),
                command.gender(),
                command.birthDate(),
                command.favoriteClubId(),
                profile.profileImageUrl(),
                command.watchStyle());
    }

    private record Tokens(String accessToken, String refreshToken) {}
}
