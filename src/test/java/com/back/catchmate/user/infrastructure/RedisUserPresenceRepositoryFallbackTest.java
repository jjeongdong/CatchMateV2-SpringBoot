package com.back.catchmate.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisUserPresenceRepositoryFallbackTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisUserPresenceRepository sut;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("Redis 장애 시 포커스 방 일괄 조회는 빈 결과로 알림 발송을 막지 않는다")
    void findFocusRoomsFallsBackToEmpty() {
        // given
        given(valueOperations.multiGet(anyList())).willThrow(new RedisConnectionFailureException("down"));

        // when & then
        assertThat(sut.findFocusRooms(List.of(1L, 2L))).isEmpty();
    }

    @Test
    @DisplayName("Redis 장애 시 포커스 방 단건 조회는 빈 값이다")
    void findFocusRoomFallsBackToEmpty() {
        // given
        given(valueOperations.get(anyString())).willThrow(new RedisConnectionFailureException("down"));

        // when & then
        assertThat(sut.findFocusRoom(1L)).isEmpty();
    }

    @Test
    @DisplayName("Redis 장애 시 온라인 표시는 예외를 던지지 않는다")
    void markOnlineSwallowsFailure() {
        // given
        willThrow(new RedisConnectionFailureException("down"))
                .given(valueOperations)
                .set(anyString(), anyString());

        // when & then
        assertThatCode(() -> sut.markOnline(1L)).doesNotThrowAnyException();
    }
}
