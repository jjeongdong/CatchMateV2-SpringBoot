package com.back.catchmate.auth.domain;

public interface RefreshTokenRepository {

    void save(String refreshToken, Long userId, long ttlMillis);

    boolean exists(String refreshToken);

    void delete(String refreshToken);
}
