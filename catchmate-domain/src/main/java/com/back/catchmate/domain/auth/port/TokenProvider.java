package com.back.catchmate.domain.auth.port;

import com.back.catchmate.domain.user.model.Authority;

public interface TokenProvider {
    String createAccessToken(Long userId, Authority role);

    String createRefreshToken(Long userId, Authority role);

    Long getUserId(String token);

    String getUserRole(String token);

    Long getRefreshTokenExpirationTime();
}
