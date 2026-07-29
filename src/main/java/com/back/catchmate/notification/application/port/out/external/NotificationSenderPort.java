package com.back.catchmate.notification.application.port.out.external;

import java.util.Map;

/*
 * 사용자에게 PUSH 알림을 전송하기 위한 출력 포트.
 */
public interface NotificationSenderPort {
    // 같은 알림의 재발송을 식별하는 data 키(값 = outbox 행 id). 재시도·회수 사이에 값이 변하지 않는다.
    // 수신 측 중복 제거 근거이자, 구현체가 전송 계층의 알림 교체(tag/collapse) 키로 쓰는 값이다.
    String DEDUP_KEY = "dedupKey";

    void sendNotification(Long userId, String token, String title, String body, Map<String, String> data);
}
