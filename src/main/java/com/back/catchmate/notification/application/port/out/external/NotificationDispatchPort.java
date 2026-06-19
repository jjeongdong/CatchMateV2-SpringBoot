package com.back.catchmate.notification.application.port.out.external;

import java.util.Map;

/**
 * 사용자에게 실시간 알림 페이로드를 전달하기 위한 출력 포트.
 */
public interface NotificationDispatchPort {
    void dispatch(Long userId, Map<String, String> payload);
}
