package com.back.catchmate.chat.infrastructure;

import com.back.catchmate.chat.domain.event.ChatMessageBroadcastEvent;
import com.back.catchmate.global.config.data.RedisTopicSubscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class ChatRedisConfig {

    /*
     * 채팅 Pub/Sub 전용 RedisTemplate.
     * 공용 템플릿의 GenericJackson2Json 은 @class 타입 메타를 매 메시지에 심어(리플렉션 기반) 무겁다.
     * 채팅 브로드캐스트는 타입이 ChatMessageBroadcastEvent 로 고정이므로 Jackson2JsonRedisSerializer 로 @class 없이 직렬화한다.
     */
    @Bean
    public RedisTemplate<String, ChatMessageBroadcastEvent> chatPubSubRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ChatMessageBroadcastEvent> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(
                new Jackson2JsonRedisSerializer<>(pubSubObjectMapper(), ChatMessageBroadcastEvent.class));
        return template;
    }

    /**
     * 구독 측에서 String 으로 역직렬화하지 않고 Redis 원본 바이트를 그대로 넘기도록 serializer 를 비운다
     * (ChatRedisSubscriber 가 JSON 재직렬화 없이 raw 바이트를 STOMP 로 그대로 전달하기 위함).
     */
    @Bean
    public MessageListenerAdapter chatListenerAdapter(ChatRedisSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onMessage");
        adapter.setSerializer(null);
        return adapter;
    }

    @Bean
    public ChannelTopic chatTopic() {
        return new ChannelTopic("catchmate-chat-topic");
    }

    @Bean
    public RedisTopicSubscription chatSubscription(MessageListenerAdapter chatListenerAdapter, ChannelTopic chatTopic) {
        return new RedisTopicSubscription(chatListenerAdapter, chatTopic);
    }

    // 방송 JSON 의 날짜는 옛 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") 과 같은 형식이어야 한다 (서버 간 계약).
    static ObjectMapper pubSubObjectMapper() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(module);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}
