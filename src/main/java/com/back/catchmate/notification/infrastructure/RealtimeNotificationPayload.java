package com.back.catchmate.notification.infrastructure;

import java.util.List;
import java.util.Map;

/**
 * Redis Pub/Sub 로 인스턴스 간에 오가는 실시간 알림 (옛 NotificationEvent, JSON 필드 그대로).
 * <p>
 * 수신자를 <b>목록</b>으로 담는다. 같은 내용을 여러 명에게 보낼 때(공지·채팅방 팬아웃)
 * 수신자마다 PUBLISH 하면 왕복이 N번 생기고 각 인스턴스가 N건을 역직렬화해야 한다.
 * 한 건에 모아 보내면 왕복·역직렬화가 1회로 줄고, 수신 인스턴스는 메모리에서 자기 세션에만 분배한다.
 */
public record RealtimeNotificationPayload(List<Long> userIds, Map<String, String> data) {
    public static RealtimeNotificationPayload of(List<Long> userIds, Map<String, String> data) {
        return new RealtimeNotificationPayload(userIds, data);
    }
}
