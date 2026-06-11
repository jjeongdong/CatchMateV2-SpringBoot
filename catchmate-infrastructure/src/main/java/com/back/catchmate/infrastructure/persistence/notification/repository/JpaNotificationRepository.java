package com.back.catchmate.infrastructure.persistence.notification.repository;

import com.back.catchmate.infrastructure.persistence.notification.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaNotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findAllByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndRead(Long userId, boolean read);

//    @Modifying(clearAutomatically = true)
//    @Query("UPDATE NotificationEntity n SET n.read = true WHERE n.userId = :userId AND n.read = false")
//    int markAllRead(@Param("userId") Long userId);
}
