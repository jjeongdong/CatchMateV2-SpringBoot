package com.back.catchmate.auth.application.port.out;

import com.back.catchmate.oauth.domain.model.SignupTokenClaims;
import com.back.catchmate.user.domain.model.Authority;

public interface TokenProvider {
    String createAccessToken(Long userId, Authority role);

    String createRefreshToken(Long userId, Authority role);

    Long getUserId(String token);

    String getUserRole(String token);

    Long getRefreshTokenExpirationTime();

    String createSignupToken(SignupTokenClaims claims);

    SignupTokenClaims parseSignupToken(String signupToken);

    Long getSignupTokenExpirationTime();
}
