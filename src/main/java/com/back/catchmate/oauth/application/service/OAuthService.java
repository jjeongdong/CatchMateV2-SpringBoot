package com.back.catchmate.oauth.application.service;

import com.back.catchmate.auth.application.port.out.TokenProvider;
import com.back.catchmate.auth.domain.model.AuthToken;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.oauth.application.dto.command.OAuthCallbackCommand;
import com.back.catchmate.oauth.application.dto.command.SignUpCommand;
import com.back.catchmate.oauth.application.dto.response.AuthorizeRedirect;
import com.back.catchmate.oauth.application.dto.response.OAuthCallbackResult;
import com.back.catchmate.oauth.application.dto.response.SignUpResponse;
import com.back.catchmate.oauth.application.dto.response.SignUpResult;
import com.back.catchmate.oauth.application.port.in.OAuthUseCase;
import com.back.catchmate.oauth.application.port.out.AuthFetchPort;
import com.back.catchmate.oauth.application.port.out.ClubFetchPort;
import com.back.catchmate.oauth.application.port.out.OAuthClient;
import com.back.catchmate.oauth.application.port.out.OAuthClientRegistry;
import com.back.catchmate.oauth.application.port.out.UserFetchPort;
import com.back.catchmate.oauth.domain.model.OAuthUserInfo;
import com.back.catchmate.oauth.domain.model.SignupTokenClaims;
import com.back.catchmate.user.domain.enums.Provider;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OAuthService implements OAuthUseCase {

    private final OAuthClientRegistry oauthClientRegistry;
    private final TokenProvider tokenProvider;

    private final AuthFetchPort authFetchPort;
    private final ClubFetchPort clubFetchPort;
    private final UserFetchPort userFetchPort;

    public AuthorizeRedirect buildAuthorizeRedirect(Provider provider) {
        OAuthClient client = oauthClientRegistry.get(provider);
        String state = UUID.randomUUID().toString();
        String url = client.buildAuthorizeUrl(state);
        return new AuthorizeRedirect(url, state);
    }

    @Override
    @Transactional
    public OAuthCallbackResult handleCallback(OAuthCallbackCommand command) {
        validateState(command.state(), command.stateFromCookie());

        OAuthClient client = oauthClientRegistry.get(command.provider());
        OAuthUserInfo userInfo = client.exchange(command.code());

        Optional<User> userOptional = userFetchPort.findByProviderId(userInfo.getProviderIdWithProvider());
        if (userOptional.isPresent()) {
            AuthToken token = authFetchPort.createToken(userOptional.get());
            return new OAuthCallbackResult.Existing(token.getAccessToken(), token.getRefreshToken());
        }

        String signupToken = tokenProvider.createSignupToken(SignupTokenClaims.from(userInfo));
        return new OAuthCallbackResult.NewUser(signupToken);
    }

    @Override
    @Transactional
    public SignUpResult signUp(SignUpCommand command) {
        SignupTokenClaims claims = tokenProvider.parseSignupToken(command.signupToken());
        Long favoriteClubId = command.favoriteClubId();
        // validate club exists
        clubFetchPort.getClub(favoriteClubId);

        User user = User.createUser(
                claims.getProvider(),
                claims.getProviderIdWithProvider(),
                claims.getEmail(),
                command.nickName(),
                command.gender(),
                command.birthDate(),
                favoriteClubId,
                claims.getProfileImageUrl(),
                null,
                command.watchStyle()
        );
        User savedUser = userFetchPort.createUser(user);

        AuthToken token = authFetchPort.createToken(savedUser);
        SignUpResponse response = SignUpResponse.of(
                savedUser.getId(),
                token.getAccessToken(),
                savedUser.getCreatedAt()
        );
        return new SignUpResult(response, token.getRefreshToken());
    }

    private void validateState(String state, String stateFromCookie) {
        if (state == null || stateFromCookie == null || !state.equals(stateFromCookie)) {
            throw new BaseException(ErrorCode.OAUTH_STATE_MISMATCH);
        }
    }
}
