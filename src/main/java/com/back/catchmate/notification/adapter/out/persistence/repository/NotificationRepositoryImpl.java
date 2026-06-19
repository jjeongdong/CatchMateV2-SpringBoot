package com.back.catchmate.notification.adapter.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.notification.domain.model.Notification;
import com.back.catchmate.notification.application.port.out.persistence.NotificationRepository;
import com.back.catchmate.notification.adapter.out.persistence.entity.NotificationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {
    private final JpaNotificationRepository jpaNotificationRepository;

    @Override
    @Transactional
    public Notification save(Notification notification) {
        NotificationEntity entity = NotificationEntity.from(notification);
        return jpaNotificationRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Notification> findById(Long notificationId) {
        return jpaNotificationRepository.findById(notificationId)
                .map(NotificationEntity::toDomain);
    }

    @Override
    public Page<Notification> findAllByUserId(Long userId, Pageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPageNumber(),
                domainPageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<NotificationEntity> entityPage = jpaNotificationRepository.findAllByUserId(userId, pageable);

        List<Notification> domains = entityPage.getContent().stream()
                .map(NotificationEntity::toDomain)
                .toList();

        return new PageImpl<>(domains, pageable, entityPage.getTotalElements());
    }

    @Override
    public void delete(Notification notification) {
        NotificationEntity entity = NotificationEntity.from(notification);
        jpaNotificationRepository.delete(entity);
    }

    @Override
    public boolean hasUnreadNotifications(Long userId) {
        return jpaNotificationRepository.existsByUserIdAndRead(userId, false);
    }

    @Override
    @Transactional
    public int markAllRead(Long userId) {
        return jpaNotificationRepository.markAllReadByUserId(userId);
    }
}
