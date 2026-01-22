package com.back.catchmate.domain.auth.repository;

public interface RefreshTokenRepository {
    void save(String refreshToken, Long userId, Long ttl);
    boolean existsById(String refreshToken);
    void deleteById(String refreshToken);
}
