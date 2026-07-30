package com.back.catchmate.notification.application.port.in;

import com.back.catchmate.notification.domain.model.AlarmType;

import java.util.List;

public interface NotificationInternalCommandUseCase {
    /**
     * 알림 단건 생성. 호출자(다른 컨텍스트)는 도메인 모델을 직접 만들지 않고
     * 식별자/원시 값/enum 만 전달한다.
     */
    void createNotification(Long userId, Long senderId, Long boardId, String title, AlarmType type, Long targetId);

    /**
     * 동일 내용의 알림을 여러 수신자에게 일괄 생성한다(공지 브로드캐스트 등).
     * 수신자별로 달라지는 값은 {@code userIds} 뿐이고 나머지는 전원 공유한다.
     * 단건 생성을 반복하면 IDENTITY 전략 탓에 수신자 수만큼 INSERT 왕복이 발생하므로 배치 경로를 따로 둔다.
     */
    void createNotifications(List<Long> userIds, Long senderId, Long boardId, String title, AlarmType type, Long targetId);
}
