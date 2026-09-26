package com.back.catchmate.oauth.service;

import com.back.catchmate.auth.dto.SignupTokenPayload;
import com.back.catchmate.auth.dto.response.IssuedAuthToken;
import com.back.catchmate.auth.service.AuthService;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.oauth.dto.OAuthUserInfo;
import com.back.catchmate.oauth.dto.SignupTokenClaims;
import com.back.catchmate.oauth.dto.command.OAuthCallbackCommand;
import com.back.catchmate.oauth.dto.command.SignUpCommand;
import com.back.catchmate.oauth.dto.response.AuthorizeRedirect;
import com.back.catchmate.oauth.dto.response.OAuthCallbackResult;
import com.back.catchmate.oauth.dto.response.SignUpResponse;
import com.back.catchmate.oauth.dto.response.SignUpResult;
import com.back.catchmate.oauth.entity.Provider;
import com.back.catchmate.oauth.infra.OAuthClient;
import com.back.catchmate.oauth.infra.OAuthClientRegistry;
import com.back.catchmate.user.application.UserCommandService;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import com.back.catchmate.user.application.dto.command.UserCreateCommand;
import com.back.catchmate.user.application.dto.result.UserCreateResult;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 클래스 레벨에 @Transactional 을 붙이지 말 것.
// handleCallback 은 OAuth 공급자(카카오·구글)로 토큰 교환 + 사용자 조회 HTTP 를 순차로 호출한다.
// 여기에 트랜잭션이 걸리면 read timeout(5초) 동안 DB 커넥션을 붙잡은 채 외부 응답을 기다리게 되고,
// 인스턴스당 pool=15 이므로 로그인 트래픽이 몰리면 풀이 말라 서비스 전체가 멈춘다.
// 트랜잭션은 DB 를 실제로 건드리는 메서드에만 선언한다.
@Service
@RequiredArgsConstructor
public class OAuthService {
    private final OAuthClientRegistry oauthClientRegistry;

    private final UserQueryApi userQueryApi;
    private final UserCommandService userCommandService;
    private final ClubQueryApi clubQueryApi;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public AuthorizeRedirect buildAuthorizeRedirect(Provider provider) {
        OAuthClient client = oauthClientRegistry.get(provider);
        String state = UUID.randomUUID().toString();
        String url = client.buildAuthorizeUrl(state);
        return new AuthorizeRedirect(url, state);
    }

    public OAuthCallbackResult handleCallback(OAuthCallbackCommand command) {
        validateStateMatches(command.state(), command.stateFromCookie());

        OAuthUserInfo oauthUserInfo = fetchOAuthUserInfo(command);
        Optional<UserInfo> registeredUser =
                userQueryApi.findInfoByProviderId(oauthUserInfo.getProviderIdWithProvider());

        if (registeredUser.isEmpty()) {
            return issueSignupToken(oauthUserInfo);
        }

        return issueLoginTokens(registeredUser.get());
    }

    @Transactional
    public SignUpResult signUp(SignUpCommand command) {
        SignupTokenClaims claims = parseSignupToken(command.signupToken());
        // 존재하지 않으면 ClubQueryApi 가 ClubNotFoundException 을 던진다
        clubQueryApi.getInfo(command.favoriteClubId());

        UserCreateResult createdUser = userCommandService.createUser(toCreateUserCommand(claims, command));
        IssuedAuthToken issuedToken = authService.createToken(createdUser.userId(), createdUser.authority());

        SignUpResponse response =
                SignUpResponse.of(createdUser.userId(), issuedToken.accessToken(), createdUser.createdAt());
        return new SignUpResult(response, issuedToken.refreshToken());
    }

    private OAuthUserInfo fetchOAuthUserInfo(OAuthCallbackCommand command) {
        OAuthClient client = oauthClientRegistry.get(command.provider());
        return client.exchange(command.code());
    }

    private OAuthCallbackResult issueLoginTokens(UserInfo user) {
        IssuedAuthToken issuedToken = authService.createToken(user.userId(), user.authority());
        return new OAuthCallbackResult.Existing(issuedToken.accessToken(), issuedToken.refreshToken());
    }

    private OAuthCallbackResult issueSignupToken(OAuthUserInfo oauthUserInfo) {
        SignupTokenClaims claims = SignupTokenClaims.from(oauthUserInfo);
        SignupTokenPayload payload = new SignupTokenPayload(
                claims.getProvider() != null ? claims.getProvider().getProvider() : null,
                claims.getProviderId(),
                claims.getEmail(),
                claims.getProfileImageUrl());
        String signupToken = authService.issueSignupToken(payload);
        return new OAuthCallbackResult.NewUser(signupToken);
    }

    private SignupTokenClaims parseSignupToken(String signupToken) {
        SignupTokenPayload payload = authService.parseSignupToken(signupToken);
        return SignupTokenClaims.builder()
                .provider(payload.provider() != null ? Provider.of(payload.provider()) : null)
                .providerId(payload.providerId())
                .email(payload.email())
                .profileImageUrl(payload.profileImageUrl())
                .build();
    }

    private UserCreateCommand toCreateUserCommand(SignupTokenClaims claims, SignUpCommand command) {
        return new UserCreateCommand(
                claims.getProvider().getProvider(),
                claims.getProviderIdWithProvider(),
                claims.getEmail(),
                command.nickName(),
                command.gender(),
                command.birthDate(),
                command.favoriteClubId(),
                claims.getProfileImageUrl(),
                command.watchStyle());
    }

    private void validateStateMatches(String state, String stateFromCookie) {
        if (state == null || stateFromCookie == null || !state.equals(stateFromCookie)) {
            throw new BaseException(ErrorCode.OAUTH_STATE_MISMATCH);
        }
    }
}
