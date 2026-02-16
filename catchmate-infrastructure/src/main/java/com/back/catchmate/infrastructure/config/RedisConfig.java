package com.back.catchmate.infrastructure.config;

import com.back.catchmate.infrastructure.redis.RedisSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

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

        // 1. 채팅 메시지
        container.addMessageListener(chatListenerAdapter, chatTopic);
        // 2. 알림 메시지
        container.addMessageListener(notificationListenerAdapter, notificationTopic);
        return container;
    }

    /**
     * 채팅 방 내부에 있을 경우에 사용하는 채팅용 메시지 리스너 어댑터
     */
    @Bean
    public MessageListenerAdapter chatListenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    /**
     * 채팅용 단일 Topic 생성
     */
    @Bean
    public ChannelTopic chatTopic() {
        return new ChannelTopic("catchmate-chat-topic");
    }

    /**
     * 앱 안에 있는데 실시간 반영을 위한 알림용 메시지 리스너 어댑터
     */
    @Bean
    public MessageListenerAdapter notificationListenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onNotification");
    }

    /**
     * 알림용 단일 Topic 생성
     */
    @Bean
    public ChannelTopic notificationTopic() {
        return new ChannelTopic("catchmate-notification-topic");
    }
}
