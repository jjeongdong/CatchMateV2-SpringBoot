package com.back.catchmate.chat.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.chat.domain.MembershipSnapshot;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

// 롤링 배포 중에는 옛 서버와 새 서버가 같은 Redis 키를 함께 읽고 쓴다. 키 문자열이 바뀌면
// 포커스 방을 못 읽어 보고 있는 방에 푸시가 쏟아지고, 시퀀스가 0부터 다시 시작한다. 그래서 글자 그대로 고정한다.
@ExtendWith(MockitoExtension.class)
class ChatRedisKeyContractTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("포커스 방 키는 user:focus:{userId} 다 (user BC 시절 키와 같다)")
    void focusRoomKey() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        new RedisChatFocusRoomStore(stringRedisTemplate).focus(1L, 11L);

        // then
        then(valueOperations).should().set("user:focus:1", "11");
    }

    @Test
    @DisplayName("멤버십 캐시는 chat:member:{roomId}:{userId} 에 \"1|0\" 형식으로 10분 둔다")
    void membershipCacheKey() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        new RedisChatMembershipCache(stringRedisTemplate).put(5L, 1L, new MembershipSnapshot(true, false));

        // then
        then(valueOperations).should().set("chat:member:5:1", "1|0", Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("방 시퀀스 키는 chat:room:{roomId}:seq 다")
    void sequenceKey() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        new RedisChatSequenceStore(stringRedisTemplate).next(5L);

        // then
        then(valueOperations).should().increment("chat:room:5:seq");
    }

    @Test
    @DisplayName("방 시퀀스 버퍼는 chat:room-sequence:buffer, 읽음 버퍼는 chat:read-sequence:buffer 의 {roomId}:{userId} 필드다")
    void bufferKeys() {
        // when
        new RedisChatRoomSequenceBuffer(stringRedisTemplate).buffer(5L, 42L);
        new RedisReadSequenceBuffer(stringRedisTemplate).buffer(5L, 1L, 42L);

        // then
        then(stringRedisTemplate)
                .should()
                .execute(any(RedisScript.class), eq(List.of("chat:room-sequence:buffer")), eq("5"), eq("42"));
        then(stringRedisTemplate)
                .should()
                .execute(any(RedisScript.class), eq(List.of("chat:read-sequence:buffer")), eq("5:1"), eq("42"));
    }
}
