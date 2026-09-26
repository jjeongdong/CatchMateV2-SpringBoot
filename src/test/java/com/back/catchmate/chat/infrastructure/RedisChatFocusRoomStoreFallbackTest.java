package com.back.catchmate.chat.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

// 포커스 방 조회가 실패하면 "아무도 보고 있지 않다" 로 간주해 푸시가 계속 나가야 하고, 쓰기 실패가 WebSocket 을 끊으면 안 된다.
@ExtendWith(MockitoExtension.class)
class RedisChatFocusRoomStoreFallbackTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisChatFocusRoomStore sut;

    @Test
    @DisplayName("Redis 장애 시 포커스 방 일괄 조회는 빈 결과로 알림 발송을 막지 않는다")
    void findAllFallsBackToEmpty() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.multiGet(anyList())).willThrow(new RedisConnectionFailureException("down"));

        assertThat(sut.findAll(List.of(1L, 2L))).isEmpty();
    }

    @Test
    @DisplayName("Redis 장애 시 포커스 방 단건 조회는 빈 값이다")
    void findFallsBackToEmpty() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willThrow(new RedisConnectionFailureException("down"));

        assertThat(sut.find(1L)).isEmpty();
    }

    @Test
    @DisplayName("Redis 장애 시 포커스 기록은 예외를 던지지 않는다")
    void focusSwallowsFailure() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RedisConnectionFailureException("down"))
                .given(valueOperations)
                .set(anyString(), anyString());

        assertThatCode(() -> sut.focus(1L, 11L)).doesNotThrowAnyException();
    }
}
