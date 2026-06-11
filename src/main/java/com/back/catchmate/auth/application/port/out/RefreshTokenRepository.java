package com.back.catchmate.auth.application.port.out;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(String refreshToken, Long userId, Long ttl);

    Optional<String> findById(String refreshToken);

    boolean existsById(String refreshToken);

    void deleteById(String refreshToken);
}
