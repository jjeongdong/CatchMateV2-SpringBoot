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
            MessageListenerAdapter listenerAdapter,
            MessageListenerAdapter notificationListenerAdapter,
            ChannelTopic channelTopic,
            ChannelTopic notificationTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 채팅 메시지 토픽 리스너 등록
        container.addMessageListener(listenerAdapter, channelTopic);

        // 알림 메시지 토픽 리스너 등록
        container.addMessageListener(notificationListenerAdapter, notificationTopic);

        return container;
    }

    /**
     * 실제 메시지를 수신했을 때 실행될 메서드를 지정 (delegate 패턴)
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(RedisSubscriber subscriber) {
        // RedisSubscriber 클래스의 "onMessage" 메서드를 실행하라고 지정
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    /**
     * 채팅용 단일 Topic 생성
     * (방마다 Topic을 만들지 않고, 하나의 'chat' 채널로 모든 메시지를 받은 뒤 roomId로 필터링하는 것이 리소스 관리에 유리함)
     */
    @Bean
    public ChannelTopic channelTopic() {
        return new ChannelTopic("catchmate-chat-topic");
    }

    /**
     * 알림용 메시지 리스너 어댑터
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
