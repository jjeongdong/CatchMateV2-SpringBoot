package com.back.catchmate.auth.infrastructure;

import com.back.catchmate.auth.domain.RefreshTokenRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

// Redis 장애가 로그인·로그아웃 전체를 막지 않도록 예외를 삼키고 로그만 남긴다.
// 조회 실패는 "없음" 으로 취급해 재로그인을 유도한다.
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(String refreshToken, Long userId, long ttlMillis) {
        try {
            redisTemplate.opsForValue().set(refreshToken, String.valueOf(userId), ttlMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Redis 장애: Refresh Token 저장 실패. userId: {} - {}", userId, e.getMessage());
        }
    }

    @Override
    public boolean exists(String refreshToken) {
        try {
            return redisTemplate.opsForValue().get(refreshToken) != null;
        } catch (Exception e) {
            log.error("Redis 장애: Refresh Token 조회 실패. 재로그인 유도 - {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void delete(String refreshToken) {
        try {
            redisTemplate.delete(refreshToken);
        } catch (Exception e) {
            log.error("Redis 장애: Refresh Token 삭제(로그아웃) 실패 - {}", e.getMessage());
        }
    }
}
