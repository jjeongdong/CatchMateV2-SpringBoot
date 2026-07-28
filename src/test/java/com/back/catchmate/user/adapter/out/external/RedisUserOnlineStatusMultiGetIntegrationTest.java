package com.back.catchmate.user.adapter.out.external;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// MGET 은 "요청한 키 순서대로 값을 돌려주고, 없는 키 자리에는 null 을 채운다" 는 프로토콜 가정 위에서만
// userIds 와 짝을 맞출 수 있다. 이 가정이 깨지면 'A 의 포커스 방을 B 의 것으로 착각' 하는 조용한 버그가 되므로
// 모킹이 아니라 실제 Redis(127.0.0.1:6379)로 못 박는다.
class RedisUserOnlineStatusMultiGetIntegrationTest {

    private static final String FOCUS_KEY_PREFIX = "user:focus:";

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private RedisUserOnlineStatus sut;

    private final long userA = System.nanoTime();
    private final long userB = userA + 1;
    private final long userC = userA + 2;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("127.0.0.1", 6379));
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        sut = new RedisUserOnlineStatus(redisTemplate);
    }

    @AfterEach
    void tearDown() throws Exception {
        redisTemplate.delete(List.of(FOCUS_KEY_PREFIX + userA, FOCUS_KEY_PREFIX + userB, FOCUS_KEY_PREFIX + userC));
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("포커스 방이 있는 사용자만 정확히 자기 방 번호로 매핑되고, 없는 사용자는 결과에서 빠진다")
    void 포커스_방이_있는_사용자만_자기_방으로_매핑된다() {
        // given: 가운데(userB)만 비워 MGET 응답에 null 이 끼도록 만든다
        sut.setUserFocusRoom(userA, 11L);
        sut.setUserFocusRoom(userC, 33L);

        // when
        Map<Long, Long> focusRooms = sut.getUserFocusRooms(List.of(userA, userB, userC));

        // then
        assertThat(focusRooms).containsOnly(
                Map.entry(userA, 11L),
                Map.entry(userC, 33L)
        );
        assertThat(focusRooms).doesNotContainKey(userB);
    }

    @Test
    @DisplayName("포커스 방이 있는 사용자가 한 명도 없으면 빈 결과를 반환한다")
    void 아무도_포커스_중이_아니면_빈_결과를_반환한다() {
        // when
        Map<Long, Long> focusRooms = sut.getUserFocusRooms(List.of(userA, userB, userC));

        // then
        assertThat(focusRooms).isEmpty();
    }

    @Test
    @DisplayName("수신자가 없으면 Redis 를 호출하지 않고 빈 결과를 반환한다")
    void 수신자가_없으면_빈_결과를_반환한다() {
        assertThat(sut.getUserFocusRooms(List.of())).isEmpty();
    }
}
