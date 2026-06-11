package com.back.catchmate.notification.application.port.out;

import java.util.Map;

/**
 * 사용자에게 실시간 알림 페이로드를 전달하기 위한 출력 포트.
 *
 * <p>구현체는 멀티 인스턴스 환경에서 정확히 해당 사용자의 STOMP 세션에
 * 페이로드가 도달하도록 책임진다 (예: Redis Pub/Sub fan-out).
 * 호출자는 전송 실패를 신경 쓸 필요가 없으며, 구현체가 자체적으로
 * 로깅·복구 정책을 가진다 (Best-effort).
 */
public interface NotificationDispatchPort {

    void dispatch(Long userId, Map<String, String> payload);
}
