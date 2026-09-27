package com.back.catchmate.notification.domain;

import java.util.List;
import java.util.Map;

// 접속 중인 사용자에게 보내는 실시간 알림. 휘발성이라 실패해도 예외를 던지지 않는다 (푸시는 아웃박스가 보장).
public interface RealtimeNotificationPublisher {

    void publish(Long userId, Map<String, String> data);

    void publishAll(List<Long> userIds, Map<String, String> data);
}
