package com.back.catchmate.notification.application.port.in;

import java.util.List;
import java.util.Map;

public interface NotificationDispatchUseCase {
    void dispatch(Long userId, Map<String, String> payload);

    /**
     * 같은 내용을 여러 수신자에게 실시간 전달한다(공지 브로드캐스트·채팅방 팬아웃).
     * 수신자마다 {@link #dispatch} 를 반복하지 말고 이 메서드로 묶어야 Redis 왕복이 1회로 끝난다.
     */
    void dispatchAll(List<Long> userIds, Map<String, String> payload);
}
