package com.back.catchmate.enroll.infrastructure;

import com.back.catchmate.enroll.domain.EnrollAcceptIdempotencyStore;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisEnrollAcceptIdempotencyStore implements EnrollAcceptIdempotencyStore {
    private static final String KEY_PREFIX = "idempotent:enroll:accept:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final long ttlSeconds;

    public RedisEnrollAcceptIdempotencyStore(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${enroll.idempotency.ttl-seconds:10}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public boolean acquire(Long enrollId) {
        String key = KEY_PREFIX + enrollId;
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("[Idempotency] Redis 오류, 멱등성 검사 건너뜀. key={}", key, e);
            return true;
        }
    }

    @Override
    public void release(Long enrollId) {
        String key = KEY_PREFIX + enrollId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[Idempotency] 키 해제 실패, TTL 만료까지 대기. key={}", key, e);
        }
    }
}
