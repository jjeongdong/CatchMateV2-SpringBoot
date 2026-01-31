package com.back.catchmate.domain.auth.repository;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(String refreshToken, Long userId, Long ttl);

    Optional<String> findById(String refreshToken);

    boolean existsById(String refreshToken);

    void deleteById(String refreshToken);
}
