package com.back.catchmate.global.config.data;

import com.back.catchmate.chat.adapter.in.event.ChatRedisSubscriber;
import com.back.catchmate.notification.adapter.in.event.NotificationRedisSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import com.back.catchmate.chat.application.dto.ChatMessageListDto;
import com.back.catchmate.chat.application.event.ChatMessageBroadcastEvent;
import com.back.catchmate.user.application.dto.response.UserInternalResponse;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

@EnableCaching
@Configuration
public class RedisConfig {

    /*
     * RedisTemplate 설정
     * LocalDateTime 직렬화 문제 해결을 위해 ObjectMapper에 JavaTimeModule 등록
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 1. ObjectMapper 생성 및 Module 등록
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDate 처리
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 숫자가 아닌 문자열(ISO)로 저장

        // 2. Redis Serializer에 커스텀 ObjectMapper 적용
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        return template;
    }

    /*
     * 채팅 Pub/Sub 전용 RedisTemplate.
     * 공용 템플릿의 GenericJackson2Json 은 @class 타입 메타를 매 메시지에 심어(리플렉션 기반) 무겁다.
     * 채팅 브로드캐스트는 타입이 ChatMessageBroadcastEvent 로 고정이므로 Jackson2JsonRedisSerializer 로 @class 없이 직렬화한다.
     */
    @Bean
    public RedisTemplate<String, ChatMessageBroadcastEvent> chatPubSubRedisTemplate(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisTemplate<String, ChatMessageBroadcastEvent> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, ChatMessageBroadcastEvent.class));

        return template;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withCacheConfiguration("userInternal", createCacheConfig(objectMapper, UserInternalResponse.class))
                .withCacheConfiguration("chatHistory", createCacheConfig(objectMapper, ChatMessageListDto.class))
                .build();
    }

    private RedisCacheConfiguration createCacheConfig(ObjectMapper objectMapper, Class<?> clazz) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, clazz)));
    }

    /**
     * Redis Pub/Sub 메시지를 비동기로 수신하는 컨테이너
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter chatListenerAdapter,
            MessageListenerAdapter notificationListenerAdapter,
            ChannelTopic chatTopic,
            ChannelTopic notificationTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(redisListenerTaskExecutor());

        // 1. 채팅 메시지
        container.addMessageListener(chatListenerAdapter, chatTopic);
        // 2. 알림 메시지
        container.addMessageListener(notificationListenerAdapter, notificationTopic);
        return container;
    }

    @Bean
    public ThreadPoolTaskExecutor redisListenerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("RedisMsg-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 채팅 방 내부에 있을 경우에 사용하는 채팅용 메시지 리스너 어댑터.
     * 구독 측에서 String 으로 역직렬화하지 않고 Redis 원본 바이트를 그대로 넘기도록 serializer 를 비운다
     * (ChatRedisSubscriber 가 JSON 재직렬화 없이 raw 바이트를 STOMP 로 그대로 전달하기 위함).
     */
    @Bean
    public MessageListenerAdapter chatListenerAdapter(ChatRedisSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onMessage");
        adapter.setSerializer(null);
        return adapter;
    }

    /**
     * 앱 안에 있는데 실시간 반영을 위한 알림용 메시지 리스너 어댑터
     */
    @Bean
    public MessageListenerAdapter notificationListenerAdapter(NotificationRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onNotification");
    }

    /**
     * 채팅용 단일 Topic 생성
     */
    @Bean
    public ChannelTopic chatTopic() {
        return new ChannelTopic("catchmate-chat-topic");
    }

    /**
     * 알림용 단일 Topic 생성
     */
    @Bean
    public ChannelTopic notificationTopic() {
        return new ChannelTopic("catchmate-notification-topic");
    }
}
