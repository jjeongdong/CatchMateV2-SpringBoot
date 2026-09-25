package com.back.catchmate.auth.infra;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {
    private final RedisTemplate<String, String> redisTemplate;

    public void save(String refreshToken, Long userId, Long ttl) {
        try {
            redisTemplate.opsForValue().set(refreshToken, String.valueOf(userId), ttl, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Redis 장애: Refresh Token 저장 실패. userId: {} - {}", userId, e.getMessage());
        }
    }

    public Optional<String> findById(String refreshToken) {
        try {
            String value = redisTemplate.opsForValue().get(refreshToken);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Redis 장애: Refresh Token 조회 실패. 재로그인 유도 - {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void deleteById(String refreshToken) {
        try {
            redisTemplate.delete(refreshToken);
        } catch (Exception e) {
            log.error("Redis 장애: Refresh Token 삭제(로그아웃) 실패 - {}", e.getMessage());
        }
    }
}
