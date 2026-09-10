package com.back.catchmate.notification.repository;

import com.back.catchmate.notification.entity.Notification;

import java.util.List;

public interface NotificationRepositoryCustom {
    // 수신자 수만큼의 알림을 단일 멀티로우 INSERT 로 적재한다.
    void saveAllInBatch(List<Notification> notifications);
}
