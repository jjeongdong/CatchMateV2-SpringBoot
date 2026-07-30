package com.back.catchmate.notification.adapter.out.persistence.repository;

import com.back.catchmate.notification.domain.model.NotificationOutbox;
import com.back.catchmate.notification.application.port.out.persistence.NotificationOutboxRepository;
import com.back.catchmate.notification.adapter.out.persistence.entity.NotificationOutboxEntity;
import com.back.catchmate.notification.domain.model.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationOutboxRepositoryImpl implements NotificationOutboxRepository {
    // created_at/modified_at 은 JPA 감사(@CreatedDate/@LastModifiedDate) 기반이라 순수 JDBC INSERT 에선
    // 자동 세팅되지 않으므로 SQL 에서 직접 채운다. id 는 IDENTITY 라 컬럼에서 제외한다.
    private static final String BATCH_INSERT_SQL = """
            INSERT INTO notification_outbox
                (recipient_id, fcm_token, title, body, payload,
                 retry_count, status, error_message, created_at, modified_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    // 상태 전이로 바뀌는 컬럼만 갱신한다. 나머지(수신자·본문·payload)는 적재 후 불변이다.
    private static final String BATCH_UPDATE_SQL = """
            UPDATE notification_outbox
               SET status = ?, retry_count = ?, error_message = ?, modified_at = ?
             WHERE id = ?
            """;

    private final JpaNotificationOutboxRepository jpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public NotificationOutbox save(NotificationOutbox outbox) {
        NotificationOutboxEntity entity = NotificationOutboxEntity.from(outbox);
        return jpaRepository.save(entity).toDomain();
    }

    // 수신자 수만큼의 Outbox 를 단일 멀티로우 INSERT 로 적재한다(IDENTITY 라 Hibernate batch 불가 → JdbcTemplate).
    // JDBC URL 의 rewriteBatchedStatements=true 가 있어야 한 번의 왕복으로 재작성된다.
    @Override
    public void saveAll(List<NotificationOutbox> outboxes) {
        if (outboxes.isEmpty()) {
            return;
        }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.batchUpdate(BATCH_INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                NotificationOutbox outbox = outboxes.get(i);
                ps.setLong(1, outbox.getRecipientId());
                ps.setString(2, outbox.getRecipientAddress());
                ps.setString(3, outbox.getTitle());
                ps.setString(4, outbox.getBody());
                ps.setString(5, outbox.getPayload());
                ps.setInt(6, outbox.getRetryCount());
                ps.setString(7, outbox.getStatus().name());
                ps.setString(8, outbox.getErrorMessage());
                ps.setTimestamp(9, now);
                ps.setTimestamp(10, now);
            }

            @Override
            public int getBatchSize() {
                return outboxes.size();
            }
        });
    }

    // 선점·성공·실패 확정을 건별 save 대신 한 번의 배치로 반영한다. 배치가 커질수록(스케줄러 batch-size)
    // 건별 왕복 비용이 그대로 지연으로 쌓이기 때문이다.
    @Override
    public void updateAll(List<NotificationOutbox> outboxes) {
        if (outboxes.isEmpty()) {
            return;
        }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.batchUpdate(BATCH_UPDATE_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                NotificationOutbox outbox = outboxes.get(i);
                ps.setString(1, outbox.getStatus().name());
                ps.setInt(2, outbox.getRetryCount());
                ps.setString(3, outbox.getErrorMessage());
                ps.setTimestamp(4, now);
                ps.setLong(5, outbox.getId());
            }

            @Override
            public int getBatchSize() {
                return outboxes.size();
            }
        });
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
    public List<NotificationOutbox> findAllStuckProcessing(LocalDateTime threshold, int batchSize) {
        return jpaRepository.findAllStuckForRecovery(OutboxStatus.PROCESSING, threshold, Pageable.ofSize(batchSize)).stream()
                .map(NotificationOutboxEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<NotificationOutbox> findById(Long id) {
        return jpaRepository.findById(id)
                .map(NotificationOutboxEntity::toDomain);
    }
}
