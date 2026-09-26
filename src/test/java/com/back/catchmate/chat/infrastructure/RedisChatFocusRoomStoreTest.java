package com.back.catchmate.chat.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// MGET 은 "요청한 키 순서대로 값을 돌려주고, 없는 키 자리에는 null 을 채운다" 는 프로토콜 가정 위에서만
// userIds 와 짝을 맞출 수 있다. 이 가정이 깨지면 'A 의 포커스 방을 B 의 것으로 착각' 하는 조용한 버그가 되므로
// 모킹이 아니라 실제 Redis 로 못 박는다. 운영 Redis 에 닿지 않도록 컨테이너 주소로만 연결한다.
@Testcontainers(disabledWithoutDocker = true)
class RedisChatFocusRoomStoreTest {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static final long USER_A = 1L;
    private static final long USER_B = 2L;
    private static final long USER_C = 3L;

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private RedisChatFocusRoomStore sut;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379)));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        sut = new RedisChatFocusRoomStore(redisTemplate);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("포커스 방이 있는 사용자만 정확히 자기 방 번호로 매핑되고, 없는 사용자는 결과에서 빠진다")
    void mapsOnlyFocusedUsersToTheirRooms() {
        // given: 가운데(USER_B)만 비워 MGET 응답에 null 이 끼도록 만든다
        sut.focus(USER_A, 11L);
        sut.focus(USER_C, 33L);

        // when
        Map<Long, Long> focusRooms = sut.findAll(List.of(USER_A, USER_B, USER_C));

        // then
        assertThat(focusRooms).containsOnly(Map.entry(USER_A, 11L), Map.entry(USER_C, 33L));
    }

    @Test
    @DisplayName("수신자가 없으면 빈 결과를 반환한다")
    void emptyInputReturnsEmpty() {
        assertThat(sut.findAll(List.of())).isEmpty();
    }

    @Test
    @DisplayName("포커스를 해제하면 단건 조회가 빈 값이다")
    void unfocusClearsRoom() {
        // given
        sut.focus(USER_A, 11L);

        // when
        sut.unfocus(USER_A);

        // then
        assertThat(sut.find(USER_A)).isEmpty();
    }
}
