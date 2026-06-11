package com.back.catchmate.orchestration.oauth;

import com.back.catchmate.application.auth.service.AuthService;
import com.back.catchmate.application.user.service.UserService;
import com.back.catchmate.domain.auth.model.AuthToken;
import com.back.catchmate.domain.auth.port.TokenProvider;
import com.back.catchmate.domain.oauth.model.OAuthUserInfo;
import com.back.catchmate.domain.oauth.model.SignupTokenClaims;
import com.back.catchmate.domain.oauth.port.OAuthClient;
import com.back.catchmate.domain.oauth.port.OAuthClientRegistry;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import com.back.catchmate.orchestration.oauth.dto.command.OAuthCallbackCommand;
import com.back.catchmate.orchestration.oauth.dto.response.AuthorizeRedirect;
import com.back.catchmate.orchestration.oauth.dto.response.OAuthCallbackResult;
import com.back.catchmate.user.enums.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OAuthOrchestrator {
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
