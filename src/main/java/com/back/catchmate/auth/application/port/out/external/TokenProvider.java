package com.back.catchmate.auth.application.port.out.external;

import com.back.catchmate.auth.application.dto.SignupTokenPayload;

public interface TokenProvider {
    String createAccessToken(Long userId, String role);

    String createRefreshToken(Long userId, String role);

    Long getUserId(String token);

    String getUserRole(String token);

    Long getRefreshTokenExpirationTime();

    String createSignupToken(SignupTokenPayload payload);

    SignupTokenPayload parseSignupToken(String signupToken);
}
