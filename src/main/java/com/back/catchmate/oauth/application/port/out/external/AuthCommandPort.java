package com.back.catchmate.oauth.application.port.out.external;

import com.back.catchmate.oauth.application.dto.response.IssuedTokenPair;
import com.back.catchmate.oauth.domain.model.SignupTokenClaims;

public interface AuthCommandPort {
    IssuedTokenPair createToken(Long userId, String authority);

    String issueSignupToken(SignupTokenClaims claims);
}
