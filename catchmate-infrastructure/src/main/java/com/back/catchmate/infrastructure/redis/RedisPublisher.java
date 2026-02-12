package com.back.catchmate.infrastructure.redis;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.application.chat.port.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher implements MessagePublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;
    private final ChannelTopic notificationTopic;

    @Override
    public void publish(String topicName, ChatMessageEvent message) {
        // Redis Topic으로 메시지 객체를 직렬화(JSON)하여 전송
        // topicName을 인자로 받았지만, 여기서는 설정된 단일 Topic을 사용합니다.
        redisTemplate.convertAndSend(topicName, message);
    }

    @Override
    public void publishNotification(Long userId, Map<String, String> data) {
        try {
            // Redis로 보낼 객체 포장 (User ID가 꼭 필요함)
            Map<String, Object> payload = Map.of(
                    "userId", userId,
                    "data", data
            );

            redisTemplate.convertAndSend(notificationTopic.getTopic(), payload);
        } catch (Exception e) {
            log.error("Failed to publish notification to Redis", e);
            // Redis 실패 시에도 로직이 멈추지 않도록 예외 처리를 하거나,
            // 여기서 throw해서 호출부에서 FCM만이라도 보내게 할 수 있음
        }
    }
}
