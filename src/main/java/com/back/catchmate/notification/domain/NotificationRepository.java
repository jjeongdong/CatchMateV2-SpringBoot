package com.back.catchmate.notification.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository {

    Notification save(Notification notification);

    Notification getById(Long notificationId);

    void delete(Notification notification);

    // 수신자 수만큼의 알림을 한 번의 멀티로우 INSERT 로 적재한다.
    void saveAllInBatch(List<Notification> notifications);

    // 최신순(createdAt, id 내림차순). 커서가 null 이면 처음부터, 아니면 커서보다 오래된 것부터 limit 개.
    List<Notification> findPageByUserId(Long userId, LocalDateTime cursorCreatedAt, Long cursorId, int limit);

    boolean existsUnreadByUserId(Long userId);

    int markAllReadByUserId(Long userId);
}
