package com.back.catchmate.enroll.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisEnrollAcceptIdempotencyStoreTest {

    private static final String KEY = "idempotent:enroll:accept:100";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisEnrollAcceptIdempotencyStore store;

    @BeforeEach
    void setUp() {
        store = new RedisEnrollAcceptIdempotencyStore(redisTemplate, 10L);
    }

    @Test
    @DisplayName("키를 선점하면 true 를 반환하고 지정한 TTL 로 저장한다")
    void acquires() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(KEY, "1", Duration.ofSeconds(10))).willReturn(Boolean.TRUE);

        assertThat(store.acquire(100L)).isTrue();
        then(valueOperations).should().setIfAbsent(KEY, "1", Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("이미 선점된 키면 false")
    void alreadyAcquired() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(KEY, "1", Duration.ofSeconds(10))).willReturn(Boolean.FALSE);

        assertThat(store.acquire(100L)).isFalse();
    }

    @Test
    @DisplayName("Redis 응답이 null 이면 선점 실패로 처리한다")
    void nullMeansNotAcquired() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(KEY, "1", Duration.ofSeconds(10))).willReturn(null);

        assertThat(store.acquire(100L)).isFalse();
    }

    @Test
    @DisplayName("Redis 오류가 나면 멱등성 검사를 건너뛰고 true")
    void redisDownPassesThrough() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RedisConnectionFailureException("down"))
                .given(valueOperations)
                .setIfAbsent(any(), any(), any(Duration.class));

        assertThat(store.acquire(100L)).isTrue();
    }

    @Test
    @DisplayName("해제하면 키를 삭제한다")
    void releases() {
        store.release(100L);

        then(redisTemplate).should().delete(KEY);
    }

    @Test
    @DisplayName("해제 중 Redis 오류가 나도 예외를 던지지 않는다")
    void releaseFailureIsSwallowed() {
        willThrow(new RedisConnectionFailureException("down"))
                .given(redisTemplate)
                .delete(KEY);

        assertThatCode(() -> store.release(100L)).doesNotThrowAnyException();
    }
}
