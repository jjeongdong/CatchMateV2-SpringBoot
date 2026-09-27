package com.back.catchmate.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

// 롤링 배포 중 옛 서버(NotificationEvent)와 새 서버(RealtimeNotificationPayload)가 같은 토픽을 쓴다.
class RealtimeNotificationPayloadCompatibilityTest {

    // Spring Boot 기본 ObjectMapper 와 같은 설정 (모르는 필드 무시).
    private final ObjectMapper subscriberMapper =
            Jackson2ObjectMapperBuilder.json().build();

    @Test
    @DisplayName("공용 RedisTemplate 직렬화 결과에 클래스 이름이 실리지 않아 옛 서버도 같은 JSON 을 읽는다")
    void publishesPlainJson() throws Exception {
        // given — RedisConfig.redisTemplate 과 같은 값 직렬화기
        ObjectMapper redisMapper = new ObjectMapper();
        redisMapper.registerModule(new JavaTimeModule());
        redisMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisMapper);

        // when
        String json = new String(
                serializer.serialize(RealtimeNotificationPayload.of(List.of(1L, 2L), Map.of("type", "CHAT"))),
                StandardCharsets.UTF_8);

        // then
        assertThat(json).doesNotContain("@class").doesNotContain("RealtimeNotificationPayload");
        assertThat(subscriberMapper.readTree(json).get("userIds").get(1).asLong())
                .isEqualTo(2L);
    }

    @Test
    @DisplayName("옛 서버가 보낸 JSON 을 새 타입으로 읽는다")
    void readsLegacyJson() throws Exception {
        String legacy = "{\"userIds\":[7],\"data\":{\"type\":\"NOTICE\",\"noticeId\":\"3\"}}";

        RealtimeNotificationPayload payload = subscriberMapper.readValue(legacy, RealtimeNotificationPayload.class);

        assertThat(payload.userIds()).containsExactly(7L);
        assertThat(payload.data()).containsEntry("noticeId", "3");
    }
}
