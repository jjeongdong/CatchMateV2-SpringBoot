package com.back.catchmate.global.config.data;

import com.back.catchmate.notification.event.NotificationRedisSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
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
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

    // BC 별 캐시 설정(예: chatHistory 의 값 타입)은 각 BC 가 RedisCacheManagerBuilderCustomizer 빈으로 등록한다.
    @Bean
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory, ObjectProvider<RedisCacheManagerBuilderCustomizer> customizers) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper)));

        RedisCacheManager.RedisCacheManagerBuilder builder =
                RedisCacheManager.builder(connectionFactory).cacheDefaults(defaultCacheConfig);
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    /**
     * Redis Pub/Sub 메시지를 비동기로 수신하는 컨테이너. 구독은 각 BC 가 RedisTopicSubscription 빈으로 등록한다.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory, List<RedisTopicSubscription> subscriptions) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(redisListenerTaskExecutor());
        subscriptions.forEach(
                subscription -> container.addMessageListener(subscription.listener(), subscription.topic()));
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
     * 앱 안에 있는데 실시간 반영을 위한 알림용 메시지 리스너 어댑터
     */
    @Bean
    public MessageListenerAdapter notificationListenerAdapter(NotificationRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onNotification");
    }

    /**
     * 알림용 단일 Topic 생성
     */
    @Bean
    public ChannelTopic notificationTopic() {
        return new ChannelTopic("catchmate-notification-topic");
    }

    // notification 은 아직 전환 전이라 구독 등록을 여기 둔다 (notification 전환 때 그쪽 설정으로 옮긴다).
    @Bean
    public RedisTopicSubscription notificationSubscription(
            MessageListenerAdapter notificationListenerAdapter, ChannelTopic notificationTopic) {
        return new RedisTopicSubscription(notificationListenerAdapter, notificationTopic);
    }
}
