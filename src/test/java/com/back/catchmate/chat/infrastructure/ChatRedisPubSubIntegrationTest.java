package com.back.catchmate.chat.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.chat.domain.MessageType;
import com.back.catchmate.chat.domain.event.ChatMessageBroadcastEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// 실제 Redis(이 테스트 전용 컨테이너)로 발행 -> 구독까지 전 구간을 검증한다.
// 로컬 Redis 는 local 프로필 설정(비밀번호 있음)에 맞춰 떠 있을 수 있어, 그것에 기대지 않고 전용 컨테이너를 띄운다.
// ChatRedisSubscriberTest 는 onMessage 를 직접 호출하는 단위 테스트라 MessageListenerAdapter.setSerializer(null)
// 설정이 실제 Redis 네트워크 I/O 상에서도 byte[] 를 훼손 없이 onMessage(byte[]) 로 전달하는지는 검증하지 못한다.
// 이 테스트는 RedisConfig 와 동일한 배선(직렬화 없는 MessageListenerAdapter)을 실제 Redis 로 재현해 그 부분을 확인한다.
@Testcontainers(disabledWithoutDocker = true)
class ChatRedisPubSubIntegrationTest {

    private static final int REDIS_PORT = 6379;

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(REDIS_PORT);

    private static final ChannelTopic TEST_TOPIC = new ChannelTopic("catchmate-chat-topic-test");

    private final ObjectMapper objectMapper = ChatRedisConfig.pubSubObjectMapper();

    private LettuceConnectionFactory connectionFactory;
    private RedisMessageListenerContainer container;
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(REDIS_PORT)));
        connectionFactory.afterPropertiesSet();

        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        ChatRedisSubscriber subscriber = new ChatRedisSubscriber(objectMapper, messagingTemplate);

        // RedisConfig.chatListenerAdapter 와 동일 설정: 구독측에서 String 으로 바꾸지 않고 raw byte[] 그대로 전달
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onMessage");
        adapter.setSerializer(null);
        adapter.afterPropertiesSet();

        container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(adapter, TEST_TOPIC);
        container.afterPropertiesSet();
        container.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        container.destroy();
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("Redis 로 발행한 채팅 메시지가 원본 JSON 바이트 그대로 STOMP 전송 직전까지 도달한다")
    void 발행한_메시지가_원본_그대로_구독측에_도달한다() throws Exception {
        // given: RedisChatMessageBroadcaster 가 실제로 쓰는 것과 동일한 방식으로 발행용 템플릿 구성
        RedisTemplate<String, ChatMessageBroadcastEvent> publishTemplate = new RedisTemplate<>();
        publishTemplate.setConnectionFactory(connectionFactory);
        publishTemplate.setKeySerializer(new StringRedisSerializer());
        publishTemplate.setValueSerializer(
                new Jackson2JsonRedisSerializer<>(objectMapper, ChatMessageBroadcastEvent.class));
        publishTemplate.afterPropertiesSet();

        ChatMessageBroadcastEvent event = new ChatMessageBroadcastEvent(
                100L,
                55L,
                3L,
                "동훈",
                null,
                "실제 Redis 통합 테스트",
                MessageType.TEXT,
                LocalDateTime.of(2026, 7, 28, 15, 0, 0));
        byte[] expectedBytes = objectMapper.writeValueAsBytes(event);

        CountDownLatch received = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
                    received.countDown();
                    return null;
                })
                .when(messagingTemplate)
                .send(any(), any());

        // when
        publishTemplate.convertAndSend(TEST_TOPIC.getTopic(), event);
        boolean deliveredInTime = received.await(5, TimeUnit.SECONDS);

        // then
        assertThat(deliveredInTime).isTrue();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Message<byte[]>> captor = ArgumentCaptor.forClass(Message.class);
        then(messagingTemplate).should().send(eq("/sub/chat/room/55"), captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo(expectedBytes);
    }
}
