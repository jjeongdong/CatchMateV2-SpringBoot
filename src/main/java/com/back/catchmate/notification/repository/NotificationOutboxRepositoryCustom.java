package com.back.catchmate.notification.repository;

import com.back.catchmate.notification.entity.NotificationOutbox;

import java.util.List;

public interface NotificationOutboxRepositoryCustom {
    // 수신자 수만큼의 Outbox 를 단일 멀티로우 INSERT 로 적재한다.
    void saveAllInBatch(List<NotificationOutbox> outboxes);

    // 이미 적재된 행들의 상태 전이(선점·성공·실패)를 일괄 반영한다. 건별 save 왕복을 줄이기 위한 경로다.
    void updateAll(List<NotificationOutbox> outboxes);
}
