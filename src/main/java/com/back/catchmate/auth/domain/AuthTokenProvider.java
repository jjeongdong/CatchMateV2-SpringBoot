package com.back.catchmate.auth.domain;

public interface AuthTokenProvider {

    String createAccessToken(Long userId, String role);

    String createRefreshToken(Long userId, String role);

    // 서명·만료가 유효하지 않거나 토큰이 비었으면 InvalidTokenException.
    Long getUserId(String token);

    long refreshTokenTtlMillis();

    String createSignupToken(OAuthProfile profile);

    // signup token 이 아니거나 유효하지 않으면 InvalidSignupTokenException.
    OAuthProfile parseSignupToken(String signupToken);
}
