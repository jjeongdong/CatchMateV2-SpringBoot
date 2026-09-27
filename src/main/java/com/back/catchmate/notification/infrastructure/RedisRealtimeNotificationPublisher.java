package com.back.catchmate.notification.infrastructure;

import com.back.catchmate.notification.domain.RealtimeNotificationPublisher;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRealtimeNotificationPublisher implements RealtimeNotificationPublisher {
    // 한 메시지에 담는 수신자 수 상한. 전체 유저 대상 공지에서 메시지 하나가 무한정 커지지 않게 자른다.
    private static final int BROADCAST_CHUNK_SIZE = 1_000;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic notificationTopic;

    @Override
    public void publish(Long userId, Map<String, String> data) {
        try {
            redisTemplate.convertAndSend(
                    notificationTopic.getTopic(), RealtimeNotificationPayload.of(List.of(userId), data));
        } catch (Exception e) {
            log.error("Redis Pub/Sub 장애로 실시간 알림 유실 userId={}", userId, e);
        }
    }

    // 수신자마다 publish 하면 그만큼 Redis 왕복이 생기므로 청크 단위로 묶어 보낸다.
    @Override
    public void publishAll(List<Long> userIds, Map<String, String> data) {
        for (int start = 0; start < userIds.size(); start += BROADCAST_CHUNK_SIZE) {
            int end = Math.min(start + BROADCAST_CHUNK_SIZE, userIds.size());
            List<Long> chunk = userIds.subList(start, end);
            try {
                redisTemplate.convertAndSend(notificationTopic.getTopic(), RealtimeNotificationPayload.of(chunk, data));
            } catch (Exception e) {
                // 실시간 알림은 휘발성이라 한 청크가 실패해도 나머지는 계속 보낸다(푸시는 아웃박스가 따로 보장).
                log.error("Redis Pub/Sub 장애로 실시간 알림 일부 유실 count={}", chunk.size(), e);
            }
        }
    }
}
