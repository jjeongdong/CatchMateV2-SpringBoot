package com.back.catchmate.infrastructure.auth.repository;

import com.back.catchmate.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(String refreshToken, Long userId, Long ttl) {
        redisTemplate.opsForValue().set(refreshToken, String.valueOf(userId), ttl, TimeUnit.MILLISECONDS);
    }

    @Override
    public Optional<String> findById(String refreshToken) {
        String value = redisTemplate.opsForValue().get(refreshToken);
        return Optional.ofNullable(value);
    }

    @Override
    public boolean existsById(String refreshToken) {
        return redisTemplate.hasKey(refreshToken);
    }

    @Override
    public void deleteById(String refreshToken) {
        redisTemplate.delete(refreshToken);
    }
}
