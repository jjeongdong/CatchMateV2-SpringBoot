package com.back.catchmate.notification.application.event;

import java.util.List;
import java.util.Map;

/**
 * Redis Pub/Sub 로 인스턴스 간에 오가는 실시간 알림 페이로드.
 * <p>
 * 수신자를 <b>목록</b>으로 담는다. 같은 내용을 여러 명에게 보낼 때(공지 브로드캐스트·채팅방 팬아웃)
 * 수신자마다 PUBLISH 하면 왕복이 N번 생기고 각 인스턴스가 N건을 역직렬화해야 한다.
 * 한 건에 모아 보내면 왕복·역직렬화가 1회로 줄고, 수신 인스턴스는 메모리에서 자기 세션에만 분배한다.
 * 수신자가 1명인 알림은 원소 1개짜리 목록으로 같은 경로를 탄다.
 *
 * @param userIds 알림 받을 유저 ID 목록
 * @param data    수신자 전원이 공유하는 알림 데이터
 */
public record NotificationEvent(
        List<Long> userIds,
        Map<String, String> data
) {
    public static NotificationEvent of(Long userId, Map<String, String> data) {
        return new NotificationEvent(List.of(userId), data);
    }

    public static NotificationEvent of(List<Long> userIds, Map<String, String> data) {
        return new NotificationEvent(userIds, data);
    }
}
