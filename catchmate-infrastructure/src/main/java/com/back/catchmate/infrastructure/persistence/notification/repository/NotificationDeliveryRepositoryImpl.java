package com.back.catchmate.infrastructure.persistence.notification.repository;

import com.back.catchmate.domain.notification.model.NotificationDelivery;
import com.back.catchmate.domain.notification.repository.NotificationDeliveryRepository;
import com.back.catchmate.infrastructure.persistence.notification.entity.NotificationDeliveryEntity;
import com.back.catchmate.notifications.enums.DeliveryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationDeliveryRepositoryImpl implements NotificationDeliveryRepository {
    private final JpaNotificationDeliveryRepository jpaRepository;

    @Override
    public NotificationDelivery save(NotificationDelivery delivery) {
        NotificationDeliveryEntity entity = NotificationDeliveryEntity.from(delivery);
        return jpaRepository.save(entity).toModel();
    }

    @Override
    public List<NotificationDelivery> findAllPending(int maxRetryCount) {
        return jpaRepository.findAllByStatusAndRetryCountLessThan(DeliveryStatus.PENDING, maxRetryCount)
                .stream()
                .map(NotificationDeliveryEntity::toModel)
                .toList();
    }

    @Override
    public Optional<NotificationDelivery> findById(Long id) {
        return jpaRepository.findById(id)
                .map(NotificationDeliveryEntity::toModel);
    }
}
