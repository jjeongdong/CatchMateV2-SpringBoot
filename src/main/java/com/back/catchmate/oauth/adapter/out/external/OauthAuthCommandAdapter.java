package com.back.catchmate.oauth.adapter.out.external;

import com.back.catchmate.auth.application.dto.response.IssuedAuthToken;
import com.back.catchmate.auth.application.port.in.AuthInternalCommandUseCase;
import com.back.catchmate.auth.application.dto.SignupTokenPayload;
import com.back.catchmate.oauth.application.dto.response.IssuedTokenPair;
import com.back.catchmate.oauth.application.port.out.external.AuthCommandPort;
import com.back.catchmate.oauth.domain.model.SignupTokenClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OauthAuthCommandAdapter implements AuthCommandPort {
    private final AuthInternalCommandUseCase authInternalCommandUseCase;

    @Override
    public IssuedTokenPair createToken(Long userId, String authority) {
        IssuedAuthToken token = authInternalCommandUseCase.createToken(userId, authority);
        return new IssuedTokenPair(token.accessToken(), token.refreshToken());
    }

    @Override
    public String issueSignupToken(SignupTokenClaims claims) {
        SignupTokenPayload payload = new SignupTokenPayload(
                claims.getProvider() != null ? claims.getProvider().getProvider() : null,
                claims.getProviderId(),
                claims.getEmail(),
                claims.getProfileImageUrl()
        );
        return authInternalCommandUseCase.issueSignupToken(payload);
    }
}
