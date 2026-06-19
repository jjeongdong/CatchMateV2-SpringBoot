package com.back.catchmate.notification.application.port.in;

import com.back.catchmate.notification.domain.model.AlarmType;

public interface NotificationInternalCommandUseCase {
    /**
     * 알림 단건 생성. 호출자(다른 컨텍스트)는 도메인 모델을 직접 만들지 않고
     * 식별자/원시 값/enum 만 전달한다.
     */
    void createNotification(Long userId, Long senderId, Long boardId, String title, AlarmType type, Long targetId);
}
