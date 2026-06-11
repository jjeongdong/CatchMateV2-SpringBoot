package com.back.catchmate.oauth.application.service;


import com.back.catchmate.oauth.application.port.in.OAuthUseCase;
import com.back.catchmate.auth.application.service.AuthService;
import com.back.catchmate.user.application.service.UserService;
import com.back.catchmate.auth.domain.model.AuthToken;
import com.back.catchmate.auth.application.port.out.TokenProvider;
import com.back.catchmate.oauth.domain.model.OAuthUserInfo;
import com.back.catchmate.oauth.domain.model.SignupTokenClaims;
import com.back.catchmate.oauth.application.port.out.OAuthClient;
import com.back.catchmate.oauth.application.port.out.OAuthClientRegistry;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.oauth.application.dto.command.OAuthCallbackCommand;
import com.back.catchmate.oauth.application.dto.response.AuthorizeRedirect;
import com.back.catchmate.oauth.application.dto.response.OAuthCallbackResult;
import com.back.catchmate.user.domain.enums.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OAuthService implements OAuthUseCase {
    private final OAuthClientRegistry oauthClientRegistry;
    private final AuthService authService;
    private final UserService userService;
    private final TokenProvider tokenProvider;

    public AuthorizeRedirect buildAuthorizeRedirect(Provider provider) {
        OAuthClient client = oauthClientRegistry.get(provider);
        String state = UUID.randomUUID().toString();
        String url = client.buildAuthorizeUrl(state);
        return new AuthorizeRedirect(url, state);
    }

    @Transactional
    public OAuthCallbackResult handleCallback(OAuthCallbackCommand command) {
        validateState(command.getState(), command.getStateFromCookie());

        OAuthClient client = oauthClientRegistry.get(command.getProvider());
        OAuthUserInfo userInfo = client.exchange(command.getCode());

        Optional<User> userOptional = userService.findByProviderId(userInfo.getProviderIdWithProvider());
        if (userOptional.isPresent()) {
            AuthToken token = authService.createToken(userOptional.get());
            return new OAuthCallbackResult.Existing(token.getAccessToken(), token.getRefreshToken());
        }

        String signupToken = tokenProvider.createSignupToken(SignupTokenClaims.from(userInfo));
        return new OAuthCallbackResult.NewUser(signupToken);
    }

    private void validateState(String state, String stateFromCookie) {
        if (state == null || stateFromCookie == null || !state.equals(stateFromCookie)) {
            throw new BaseException(ErrorCode.OAUTH_STATE_MISMATCH);
        }
    }
}
