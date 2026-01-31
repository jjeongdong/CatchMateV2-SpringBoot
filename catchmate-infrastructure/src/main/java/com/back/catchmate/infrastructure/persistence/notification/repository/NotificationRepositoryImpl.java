package com.back.catchmate.infrastructure.persistence.notification.repository;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.notification.model.Notification;
import com.back.catchmate.domain.notification.repository.NotificationRepository;
import com.back.catchmate.infrastructure.persistence.notification.entity.NotificationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        return jpaNotificationRepository.save(entity).toModel();
    }

    @Override
    public Optional<Notification> findById(Long notificationId) {
        return jpaNotificationRepository.findById(notificationId)
                .map(NotificationEntity::toModel);
    }

    @Override
    public DomainPage<Notification> findAllByUserId(Long userId, DomainPageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPage(),
                domainPageable.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<NotificationEntity> entityPage = jpaNotificationRepository.findAllByUserId(userId, pageable);

        List<Notification> domains = entityPage.getContent().stream()
                .map(NotificationEntity::toModel)
                .toList();

        return new DomainPage<>(
                domains,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements()
        );
    }

    @Override
    public void delete(Notification notification) {
        NotificationEntity entity = NotificationEntity.from(notification);
        jpaNotificationRepository.delete(entity);
    }
}
