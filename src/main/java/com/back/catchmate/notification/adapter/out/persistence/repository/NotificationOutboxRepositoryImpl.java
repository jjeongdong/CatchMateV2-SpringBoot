package com.back.catchmate.notification.adapter.out.persistence.repository;

import com.back.catchmate.notification.domain.model.NotificationOutbox;
import com.back.catchmate.notification.application.port.out.persistence.NotificationOutboxRepository;
import com.back.catchmate.notification.adapter.out.persistence.entity.NotificationOutboxEntity;
import com.back.catchmate.notification.domain.model.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public List<NotificationOutbox> findAllPending(int maxRetryCount, int batchSize) {
        return jpaRepository.findAllForProcessing(OutboxStatus.PENDING, maxRetryCount, Pageable.ofSize(batchSize)).stream()
                .map(NotificationOutboxEntity::toDomain)
                .toList();
    }


    @Override
    public List<NotificationOutbox> findAllPendingByRecipientId(Long recipientId) {
        return jpaRepository.findAllByRecipientIdAndStatusForProcessing(recipientId, OutboxStatus.PENDING).stream()
                .map(NotificationOutboxEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<NotificationOutbox> findById(Long id) {
        return jpaRepository.findById(id)
                .map(NotificationOutboxEntity::toDomain);
    }
}
