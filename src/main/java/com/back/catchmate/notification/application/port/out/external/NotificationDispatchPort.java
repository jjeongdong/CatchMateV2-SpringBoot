package com.back.catchmate.notification.application.port.out.external;

import java.util.List;
import java.util.Map;

/**
 * 사용자에게 실시간 알림 페이로드를 전달하기 위한 출력 포트.
 */
public interface NotificationDispatchPort {
    /**
     * 단일 수신자에게 전달한다. 수신자가 1명인 알림(Enroll·문의 답변 등)에 쓴다.
     */
    void dispatch(Long userId, Map<String, String> payload);

    /**
     * 같은 내용을 여러 수신자에게 전달한다(공지 브로드캐스트·채팅방 팬아웃).
     * <p>
     * {@link #dispatch} 를 수신자 수만큼 반복하면 그만큼 Redis 왕복이 발생하므로 한 건으로 묶어 보낸다.
     */
    void dispatchAll(List<Long> userIds, Map<String, String> payload);
}
