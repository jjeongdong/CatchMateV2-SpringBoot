package com.back.catchmate.infrastructure.chat;

import com.back.catchmate.domain.chat.port.ChatSequencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSequenceAdapter implements ChatSequencePort {
    private final StringRedisTemplate redisTemplate;

    /**
     * 채팅방 별 메시지 시퀀스 생성 (Atomic Increment)
     * Key: chat:room:{roomId}:seq
     */
    public Long generateSequence(Long roomId) {
        return redisTemplate.opsForValue()
                .increment("chat:room:" + roomId + ":seq");
    }
}
