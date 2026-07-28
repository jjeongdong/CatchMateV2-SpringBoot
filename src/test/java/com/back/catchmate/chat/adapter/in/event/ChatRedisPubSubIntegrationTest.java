package com.back.catchmate.chat.adapter.in.event;

import com.back.catchmate.chat.application.event.ChatMessageBroadcastEvent;
import com.back.catchmate.chat.domain.enums.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

// 실제 로컬 Redis(127.0.0.1:6379)로 발행 -> 구독까지 전 구간을 검증한다.
// ChatRedisSubscriberTest 는 onMessage 를 직접 호출하는 단위 테스트라 MessageListenerAdapter.setSerializer(null)
// 설정이 실제 Redis 네트워크 I/O 상에서도 byte[] 를 훼손 없이 onMessage(byte[]) 로 전달하는지는 검증하지 못한다.
// 이 테스트는 RedisConfig 와 동일한 배선(직렬화 없는 MessageListenerAdapter)을 실제 Redis 로 재현해 그 부분을 확인한다.
class ChatRedisPubSubIntegrationTest {

    private static final ChannelTopic TEST_TOPIC = new ChannelTopic("catchmate-chat-topic-test");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private LettuceConnectionFactory connectionFactory;
    private RedisMessageListenerContainer container;
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("127.0.0.1", 6379));
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
        // given: ChatMessageRedisPublisher 가 실제로 쓰는 것과 동일한 방식으로 발행용 템플릿 구성
        RedisTemplate<String, ChatMessageBroadcastEvent> publishTemplate = new RedisTemplate<>();
        publishTemplate.setConnectionFactory(connectionFactory);
        publishTemplate.setKeySerializer(new StringRedisSerializer());
        publishTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, ChatMessageBroadcastEvent.class));
        publishTemplate.afterPropertiesSet();

        ChatMessageBroadcastEvent event = ChatMessageBroadcastEvent.builder()
                .messageId(100L)
                .roomId(55L)
                .senderId(3L)
                .senderNickname("동훈")
                .content("실제 Redis 통합 테스트")
                .messageType(MessageType.TEXT)
                .createdAt(LocalDateTime.of(2026, 7, 28, 15, 0, 0))
                .build();
        byte[] expectedBytes = objectMapper.writeValueAsBytes(event);

        CountDownLatch received = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            received.countDown();
            return null;
        }).when(messagingTemplate).send(any(), any());

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
