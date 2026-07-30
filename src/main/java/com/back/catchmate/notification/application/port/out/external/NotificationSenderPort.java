package com.back.catchmate.notification.application.port.out.external;

import com.back.catchmate.notification.application.port.out.dto.NotificationMessage;
import com.back.catchmate.notification.application.port.out.dto.NotificationSendResult;

import java.util.List;
import java.util.Map;

/*
 * 사용자에게 PUSH 알림을 전송하기 위한 출력 포트.
 */
public interface NotificationSenderPort {
    // 같은 알림의 재발송을 식별하는 data 키(값 = outbox 행 id). 재시도·회수 사이에 값이 변하지 않는다.
    // 수신 측 중복 제거 근거이자, 구현체가 전송 계층의 알림 교체(tag/collapse) 키로 쓰는 값이다.
    String DEDUP_KEY = "dedupKey";

    /**
     * 단건 발송. 수신자가 1~2명인 즉시발송 경로에서 쓴다. 실패는 예외로 알린다.
     */
    void sendNotification(Long userId, String token, String title, String body, Map<String, String> data);

    /**
     * 배치 발송. 아웃박스 스케줄러처럼 다건을 한 번에 처리하는 경로에서 쓴다.
     * <p>
     * 건별 성패가 섞이므로 예외를 던지지 않고 <b>입력과 같은 순서·같은 크기</b>의 결과 목록을 돌려준다.
     * 호출자는 인덱스로 원본 메시지와 결과를 짝지어 아웃박스 상태를 확정한다.
     */
    List<NotificationSendResult> sendNotifications(List<NotificationMessage> messages);
}
