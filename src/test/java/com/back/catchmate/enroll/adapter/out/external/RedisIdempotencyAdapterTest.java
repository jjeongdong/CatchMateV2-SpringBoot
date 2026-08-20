package com.back.catchmate.enroll.adapter.out.external;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class RedisIdempotencyAdapterTest {

    private static final String KEY = "idempotent:enroll:accept:100";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisIdempotencyAdapter sut;

    // ── acquireIfAbsent ─────────────────────────────────────────────

    @Test
    @DisplayName("키를 선점하면 true 를 반환하고 지정한 TTL 로 저장한다")
    void 키_선점에_성공하면_true_와_TTL_설정() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(KEY, "1", Duration.ofSeconds(10))).willReturn(Boolean.TRUE);

        // when
        boolean acquired = sut.acquireIfAbsent(KEY, 10L);

        // then
        assertThat(acquired).isTrue();
        then(valueOperations).should().setIfAbsent(KEY, "1", Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("이미 선점된 키면 false 를 반환한다")
    void 이미_선점된_키면_false() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(KEY, "1", Duration.ofSeconds(10))).willReturn(Boolean.FALSE);

        // when
        boolean acquired = sut.acquireIfAbsent(KEY, 10L);

        // then
        assertThat(acquired).isFalse();
    }

    @Test
    @DisplayName("Redis 응답이 null 이면 선점 실패로 처리한다")
    void setIfAbsent_가_null_이면_false() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(KEY, "1", Duration.ofSeconds(10))).willReturn(null);

        // when
        boolean acquired = sut.acquireIfAbsent(KEY, 10L);

        // then
        assertThat(acquired).isFalse();
    }

    @Test
    @DisplayName("Redis 오류가 나면 멱등성 검사를 건너뛰고 true 를 반환한다")
    void Redis_장애면_멱등성_검사를_건너뛰고_통과시킨다() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RedisConnectionFailureException("down"))
                .given(valueOperations).setIfAbsent(any(), any(), any(Duration.class));

        // when
        boolean acquired = sut.acquireIfAbsent(KEY, 10L);

        // then
        assertThat(acquired).isTrue();
    }

    // ── release ─────────────────────────────────────────────────────

    @Test
    @DisplayName("락을 해제하면 해당 키를 삭제한다")
    void 해제하면_키를_삭제한다() {
        // given
        // (별도 스텁 없음 — delete 호출만 검증)

        // when
        sut.release(KEY);

        // then
        then(redisTemplate).should().delete(KEY);
    }

    @Test
    @DisplayName("락 해제 중 Redis 오류가 나도 예외를 던지지 않는다")
    void 해제_실패해도_예외를_밖으로_던지지_않는다() {
        // given
        willThrow(new RedisConnectionFailureException("down")).given(redisTemplate).delete(KEY);

        // when & then
        assertThatCode(() -> sut.release(KEY)).doesNotThrowAnyException();
        then(redisTemplate).should().delete(KEY);
    }
}
