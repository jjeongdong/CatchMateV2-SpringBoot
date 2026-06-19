package com.back.catchmate.oauth.application.port.out.external;

import com.back.catchmate.oauth.domain.model.SignupTokenClaims;

public interface AuthQueryPort {
    SignupTokenClaims parseSignupToken(String signupToken);
}
