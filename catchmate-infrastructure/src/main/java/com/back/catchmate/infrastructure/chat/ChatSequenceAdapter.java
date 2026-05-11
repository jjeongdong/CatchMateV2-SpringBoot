package com.back.catchmate.infrastructure.chat;

import com.back.catchmate.domain.chat.port.ChatSequencePort;
import com.back.catchmate.domain.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatSequenceAdapter implements ChatSequencePort {
    private final StringRedisTemplate redisTemplate;
    private final ChatMessageRepository chatMessageRepository;

    private static final String SEQ_KEY_PREFIX = "chat:room:";
    private static final String SEQ_KEY_SUFFIX = ":seq";

    /**
     * 채팅방 별 메시지 시퀀스 생성 (Atomic Increment)
     * Key: chat:room:{roomId}:seq
     */
    public Long generateSequence(Long roomId) {
        return redisTemplate.opsForValue()
                .increment(buildKey(roomId));
    }

    @Override
    public Long getCurrentSequence(Long roomId) {
        String key = buildKey(roomId);
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return Long.parseLong(value);
        }
        // Redis miss → chat_message MAX(sequence)로 복구. setIfAbsent로 동시 복구 충돌 방지.
        Long max = chatMessageRepository.findMaxSequenceByChatRoomId(roomId);
        redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(max));
        String latest = redisTemplate.opsForValue().get(key);
        return latest != null ? Long.parseLong(latest) : max;
    }

    @Override
    public Map<Long, Long> getCurrentSequences(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Map.of();
        }

        List<String> keys = roomIds.stream()
                .map(this::buildKey)
                .toList();
        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, Long> result = new HashMap<>();
        List<Long> missingRoomIds = new ArrayList<>();
        for (int i = 0; i < roomIds.size(); i++) {
            String value = values != null ? values.get(i) : null;
            if (value != null) {
                result.put(roomIds.get(i), Long.parseLong(value));
            } else {
                missingRoomIds.add(roomIds.get(i));
            }
        }

        if (!missingRoomIds.isEmpty()) {
            Map<Long, Long> dbMaxMap = chatMessageRepository.findMaxSequencesByChatRoomIds(missingRoomIds);
            for (Long roomId : missingRoomIds) {
                Long max = dbMaxMap.getOrDefault(roomId, 0L);
                redisTemplate.opsForValue().setIfAbsent(buildKey(roomId), String.valueOf(max));
                result.put(roomId, max);
            }
        }

        return result;
    }

    private String buildKey(Long roomId) {
        return SEQ_KEY_PREFIX + roomId + SEQ_KEY_SUFFIX;
    }
}
