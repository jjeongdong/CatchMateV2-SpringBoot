package com.back.catchmate.notification.adapter.out.persistence.repository;

import com.back.catchmate.notification.adapter.out.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaNotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findAllByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndRead(Long userId, boolean read);

    /**
     * 해당 사용자의 unread 알림을 일괄 read=true 로 업데이트하고 반영된 row 수 반환.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationEntity n SET n.read = true " +
            "WHERE n.userId = :userId AND n.read = false")
    int markAllReadByUserId(@Param("userId") Long userId);
}
