package com.back.catchmate.oauth.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.oauth.application.dto.command.OAuthCallbackCommand;
import com.back.catchmate.oauth.application.dto.command.RegisterUserCommand;
import com.back.catchmate.oauth.application.dto.command.SignUpCommand;
import com.back.catchmate.oauth.application.dto.response.CreatedUserSummary;
import com.back.catchmate.oauth.application.dto.response.IssuedTokenPair;
import com.back.catchmate.oauth.application.dto.response.OAuthCallbackResult;
import com.back.catchmate.oauth.application.dto.response.RegisteredUserSummary;
import com.back.catchmate.oauth.application.dto.response.SignUpResponse;
import com.back.catchmate.oauth.application.dto.response.SignUpResult;
import com.back.catchmate.oauth.application.port.in.OAuthClientCommandUseCase;
import com.back.catchmate.oauth.application.port.out.external.OAuthClient;
import com.back.catchmate.oauth.application.port.out.external.OAuthClientRegistry;
import com.back.catchmate.oauth.application.port.out.external.AuthCommandPort;
import com.back.catchmate.oauth.application.port.out.external.AuthQueryPort;
import com.back.catchmate.oauth.application.port.out.external.ClubFetchPort;
import com.back.catchmate.oauth.application.port.out.external.UserCommandPort;
import com.back.catchmate.oauth.application.port.out.external.UserFetchPort;
import com.back.catchmate.oauth.domain.model.OAuthUserInfo;
import com.back.catchmate.oauth.domain.model.SignupTokenClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuthClientCommandService implements OAuthClientCommandUseCase {
    private final OAuthClientRegistry oauthClientRegistry;
    private final ClubFetchPort clubFetchPort;
    private final UserFetchPort userFetchPort;
    private final AuthCommandPort authCommandPort;
    private final AuthQueryPort authQueryPort;
    private final UserCommandPort userCommandPort;

    @Override
    public OAuthCallbackResult handleCallback(OAuthCallbackCommand command) {
        validateStateMatches(command.state(), command.stateFromCookie());

        OAuthUserInfo oauthUserInfo = fetchOAuthUserInfo(command);
        Optional<RegisteredUserSummary> registeredUser = userFetchPort.findByProviderId(oauthUserInfo.getProviderIdWithProvider());

        if (registeredUser.isEmpty()) {
            return issueSignupToken(oauthUserInfo);
        }

        return issueLoginTokens(registeredUser.get());
    }

    @Override
    @Transactional
    public SignUpResult signUp(SignUpCommand command) {
        SignupTokenClaims claims = authQueryPort.parseSignupToken(command.signupToken());
        clubFetchPort.validateClubExists(command.favoriteClubId());

        CreatedUserSummary createdUser = userCommandPort.createUser(toRegisterUserCommand(claims, command));
        IssuedTokenPair issuedToken = authCommandPort.createToken(createdUser.userId(), createdUser.authority());

        SignUpResponse response = SignUpResponse.of(createdUser.userId(), issuedToken.accessToken(), createdUser.createdAt());
        return new SignUpResult(response, issuedToken.refreshToken());
    }

    private OAuthUserInfo fetchOAuthUserInfo(OAuthCallbackCommand command) {
        OAuthClient client = oauthClientRegistry.get(command.provider());
        return client.exchange(command.code());
    }

    private OAuthCallbackResult issueLoginTokens(RegisteredUserSummary user) {
        IssuedTokenPair issuedToken = authCommandPort.createToken(user.userId(), user.authority());
        return new OAuthCallbackResult.Existing(issuedToken.accessToken(), issuedToken.refreshToken());
    }

    private OAuthCallbackResult issueSignupToken(OAuthUserInfo oauthUserInfo) {
        String signupToken = authCommandPort.issueSignupToken(SignupTokenClaims.from(oauthUserInfo));
        return new OAuthCallbackResult.NewUser(signupToken);
    }

    private RegisterUserCommand toRegisterUserCommand(SignupTokenClaims claims, SignUpCommand command) {
        return new RegisterUserCommand(
                claims.getProvider().getProvider(),
                claims.getProviderIdWithProvider(),
                claims.getEmail(),
                command.nickName(),
                command.gender(),
                command.birthDate(),
                command.favoriteClubId(),
                claims.getProfileImageUrl(),
                command.watchStyle()
        );
    }

    private void validateStateMatches(String state, String stateFromCookie) {
        if (state == null || stateFromCookie == null || !state.equals(stateFromCookie)) {
            throw new BaseException(ErrorCode.OAUTH_STATE_MISMATCH);
        }
    }
}
