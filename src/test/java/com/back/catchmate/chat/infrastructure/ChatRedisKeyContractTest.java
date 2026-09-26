package com.back.catchmate.chat.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.back.catchmate.chat.application.ChatHistoryReader;
import com.back.catchmate.chat.domain.MembershipSnapshot;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

// 롤링 배포 중에는 옛 서버와 새 서버가 같은 Redis 키를 함께 읽고 쓴다. 키 문자열이 바뀌면
// 포커스 방을 못 읽어 보고 있는 방에 푸시가 쏟아지고, 시퀀스가 0부터 다시 시작한다. 그래서 글자 그대로 고정한다.
@ExtendWith(MockitoExtension.class)
class ChatRedisKeyContractTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedisTemplate<String, Object> objectRedisTemplate;

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

    @Test
    @DisplayName("기록 캐시 무효화는 chatHistory::{roomId}_START_* 패턴을 지운다")
    @SuppressWarnings("unchecked")
    void historyEvictPattern() {
        // given
        Cursor<String> cursor = mock(Cursor.class);
        given(objectRedisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);

        // when
        new RedisChatHistoryCache(objectRedisTemplate).evictLatestPage(5L);

        // then
        ArgumentCaptor<ScanOptions> options = ArgumentCaptor.forClass(ScanOptions.class);
        then(objectRedisTemplate).should().scan(options.capture());
        assertThat(options.getValue().getPattern()).isEqualTo("chatHistory::5_START_*");
    }

    @Test
    @DisplayName("기록 캐시 키는 {roomId}_{커서 또는 START}_{limit} 이라 무효화 패턴과 짝이 맞는다")
    void historyCacheKey() throws Exception {
        // given
        String expression = ChatHistoryReader.class
                .getMethod("read", Long.class, Long.class, int.class)
                .getAnnotation(Cacheable.class)
                .key();

        // when & then
        assertThat(evaluate(expression, 5L, null, 21)).isEqualTo("5_START_21");
        assertThat(evaluate(expression, 5L, 100L, 21)).isEqualTo("5_100_21");
    }

    private static String evaluate(String expression, Long chatRoomId, Long beforeMessageId, int limit) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("chatRoomId", chatRoomId);
        context.setVariable("beforeMessageId", beforeMessageId);
        context.setVariable("limit", limit);
        return new SpelExpressionParser().parseExpression(expression).getValue(context, String.class);
    }
}
