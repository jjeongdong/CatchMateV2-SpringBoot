package com.back.catchmate.infrastructure.redis;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.application.chat.port.MessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPublisher implements MessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;

    @Override
    public void publish(String topicName, ChatMessageEvent message) {
        // Redis Topic으로 메시지 객체를 직렬화(JSON)하여 전송
        // topicName을 인자로 받았지만, 여기서는 설정된 단일 Topic을 사용합니다.
        redisTemplate.convertAndSend(topicName, message);
    }
}
