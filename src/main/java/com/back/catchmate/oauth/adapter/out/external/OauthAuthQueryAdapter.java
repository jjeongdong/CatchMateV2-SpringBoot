package com.back.catchmate.oauth.adapter.out.external;

import com.back.catchmate.auth.application.port.in.AuthInternalQueryUseCase;
import com.back.catchmate.auth.application.dto.SignupTokenPayload;
import com.back.catchmate.oauth.application.port.out.external.AuthQueryPort;
import com.back.catchmate.oauth.domain.model.SignupTokenClaims;
import com.back.catchmate.oauth.domain.enums.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OauthAuthQueryAdapter implements AuthQueryPort {
    private final AuthInternalQueryUseCase authInternalQueryUseCase;

    @Override
    public SignupTokenClaims parseSignupToken(String signupToken) {
        SignupTokenPayload payload = authInternalQueryUseCase.parseSignupToken(signupToken);
        return SignupTokenClaims.builder()
                .provider(payload.provider() != null ? Provider.of(payload.provider()) : null)
                .providerId(payload.providerId())
                .email(payload.email())
                .profileImageUrl(payload.profileImageUrl())
                .build();
    }
}
