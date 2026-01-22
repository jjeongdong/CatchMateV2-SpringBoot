package com.back.catchmate.domain.auth.service;

public interface TokenProvider {
    String createAccessToken(Long userId);

    String createRefreshToken(Long userId);

    Long parseUserId(String token);

    Long getRefreshTokenExpiration();
}
