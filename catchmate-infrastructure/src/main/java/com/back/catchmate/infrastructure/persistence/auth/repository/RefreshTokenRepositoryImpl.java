package com.back.catchmate.infrastructure.persistence.auth.repository;

import com.back.catchmate.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(String refreshToken, Long userId, Long ttl) {
        try {
            redisTemplate.opsForValue().set(refreshToken, String.valueOf(userId), ttl, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Redis 장애: Refresh Token 저장 실패. userId: {} - {}", userId, e.getMessage());
        }
    }

    @Override
    public Optional<String> findById(String refreshToken) {
        try {
            String value = redisTemplate.opsForValue().get(refreshToken);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Redis 장애: Refresh Token 조회 실패. 재로그인 유도 - {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(String refreshToken) {
        try {
            return redisTemplate.hasKey(refreshToken);
        } catch (Exception e) {
            log.error("Redis 장애: Refresh Token 존재 여부 확인 실패 - {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void deleteById(String refreshToken) {
        try {
            redisTemplate.delete(refreshToken);
        } catch (Exception e) {
            log.error("Redis 장애: Refresh Token 삭제(로그아웃) 실패 - {}", e.getMessage());
        }
    }
}
