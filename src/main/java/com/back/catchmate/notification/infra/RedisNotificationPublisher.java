package com.back.catchmate.notification.infra;

import com.back.catchmate.notification.event.NotificationEvent;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisNotificationPublisher {
    // 한 메시지에 담는 수신자 수 상한. 전체 유저 대상 공지에서 메시지 하나가 무한정 커지지 않게 자른다.
    private static final int BROADCAST_CHUNK_SIZE = 1_000;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic notificationTopic;

    /**
     * 단일 수신자에게 전달한다. 수신자가 1명인 알림(Enroll·문의 답변 등)에 쓴다.
     */
    public void dispatch(Long userId, Map<String, String> payload) {
        try {
            redisTemplate.convertAndSend(notificationTopic.getTopic(), NotificationEvent.of(userId, payload));
        } catch (Exception e) {
            log.error("Redis Pub/Sub 장애: 알림 전송 실패. userId: {}", userId, e);
        }
    }

    /**
     * 같은 내용을 여러 수신자에게 전달한다(공지 브로드캐스트·채팅방 팬아웃).
     * <p>
     * {@link #dispatch} 를 수신자 수만큼 반복하면 그만큼 Redis 왕복이 발생하므로 한 건으로 묶어 보낸다.
     */
    public void dispatchAll(List<Long> userIds, Map<String, String> payload) {
        if (userIds.isEmpty()) {
            return;
        }

        for (int start = 0; start < userIds.size(); start += BROADCAST_CHUNK_SIZE) {
            int end = Math.min(start + BROADCAST_CHUNK_SIZE, userIds.size());
            List<Long> chunk = userIds.subList(start, end);
            try {
                redisTemplate.convertAndSend(notificationTopic.getTopic(), NotificationEvent.of(chunk, payload));
            } catch (Exception e) {
                // 실시간 알림은 휘발성이라 한 청크가 실패해도 나머지는 계속 보낸다(FCM 은 아웃박스가 따로 보장).
                log.error("Redis Pub/Sub 장애: 알림 전송 실패. 수신자 {}명 구간 유실", chunk.size(), e);
            }
        }
    }
}
