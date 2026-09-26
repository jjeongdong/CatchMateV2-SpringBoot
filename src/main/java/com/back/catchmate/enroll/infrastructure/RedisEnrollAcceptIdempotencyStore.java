package com.back.catchmate.enroll.infra;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdempotencyStore {
    private final RedisTemplate<String, Object> redisTemplate;

    public boolean acquireIfAbsent(String key, long ttlSeconds) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("[Idempotency] Redis 오류, 멱등성 검사 건너뜀. key={}", key, e);
            return true;
        }
    }

    public void release(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[Idempotency] 키 해제 실패, TTL 만료까지 대기. key={}", key, e);
        }
    }
}
