package com.back.catchmate.infrastructure.persistence.notification.repository;

import com.back.catchmate.domain.notification.model.NotificationOutbox;
import com.back.catchmate.domain.notification.repository.NotificationOutboxRepository;
import com.back.catchmate.infrastructure.persistence.notification.entity.NotificationOutboxEntity;
import com.back.catchmate.notifications.enums.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationOutboxRepositoryImpl implements NotificationOutboxRepository {
    private final JpaNotificationOutboxRepository jpaRepository;

    @Override
    public NotificationOutbox save(NotificationOutbox outbox) {
        NotificationOutboxEntity entity = NotificationOutboxEntity.from(outbox);
        return jpaRepository.save(entity).toModel();
    }

    @Override
    public List<NotificationOutbox> findAllPending(int maxRetryCount) {
        return jpaRepository.findAllByStatusAndRetryCountLessThan(OutboxStatus.PENDING, maxRetryCount)
                .stream()
                .map(NotificationOutboxEntity::toModel)
                .toList();
    }

    @Override
    public Optional<NotificationOutbox> findById(Long id) {
        return jpaRepository.findById(id)
                .map(NotificationOutboxEntity::toModel);
    }
}
