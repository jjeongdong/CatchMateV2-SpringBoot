package com.back.catchmate.auth.application.port.out.persistence;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(String refreshToken, Long userId, Long ttl);

    Optional<String> findById(String refreshToken);

    void deleteById(String refreshToken);
}
