package com.back.catchmate.infrastructure.idempotency;

import com.back.catchmate.domain.common.idempotency.IdempotencyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdempotencyAdapter implements IdempotencyPort {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean acquireIfAbsent(String key, long ttlSeconds) {
        try {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("[Idempotency] Redis 오류, 멱등성 검사 건너뜀. key={}", key, e);
            return true;
        }
    }
}
