package com.back.catchmate.notification.infrastructure;

import com.back.catchmate.notification.domain.NotificationOutbox;
import com.back.catchmate.notification.domain.NotificationOutboxRepository;
import com.back.catchmate.notification.domain.OutboxStatus;
import jakarta.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationOutboxRepositoryImpl implements NotificationOutboxRepository {
    // created_at/modified_at 은 JPA 감사 기반이라 순수 JDBC INSERT 에선 자동으로 채워지지 않아 SQL 에서 직접 넣는다.
    private static final String BATCH_INSERT_SQL =
            """
            INSERT INTO notification_outbox
                (recipient_id, fcm_token, title, body, payload,
                 retry_count, status, error_message, created_at, modified_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    // 상태 전이로 바뀌는 컬럼만 갱신한다. 나머지(수신자·본문·payload)는 적재 후 불변이다.
    private static final String BATCH_UPDATE_SQL =
            """
            UPDATE notification_outbox
               SET status = ?, retry_count = ?, error_message = ?, modified_at = ?
             WHERE id = ?
            """;

    private final NotificationOutboxJpaRepository notificationOutboxJpaRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    @Override
    public NotificationOutbox save(NotificationOutbox outbox) {
        return notificationOutboxJpaRepository.save(outbox);
    }

    // IDENTITY 라 Hibernate batch insert 가 불가능해 JdbcTemplate 로 적재한다.
    // JDBC URL 의 rewriteBatchedStatements=true 가 있어야 한 번의 왕복으로 재작성된다.
    @Override
    public void saveAllInBatch(List<NotificationOutbox> outboxes) {
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

    // 선점·결과 확정을 건별 save 대신 한 번의 배치로 반영한다. 배치가 클수록 건별 왕복이 그대로 지연으로 쌓인다.
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
        // 위 배치로 이미 반영한 행이 영속 상태로 남으면 커밋 때 더티체킹이 같은 값을 건별 UPDATE 로 다시 쓴다.
        outboxes.stream().filter(entityManager::contains).forEach(entityManager::detach);
    }

    @Override
    public List<NotificationOutbox> findPendingForUpdate(int maxRetryCount, int limit) {
        return notificationOutboxJpaRepository.findAllForProcessing(
                OutboxStatus.PENDING, maxRetryCount, Pageable.ofSize(limit));
    }

    @Override
    public List<NotificationOutbox> findPendingByRecipientIdForUpdate(Long recipientId) {
        return notificationOutboxJpaRepository.findAllByRecipientIdAndStatusForProcessing(
                recipientId, OutboxStatus.PENDING);
    }

    @Override
    public List<NotificationOutbox> findPendingByRecipientIdsForUpdate(Collection<Long> recipientIds, int limit) {
        // 빈 IN 절은 MySQL 문법 오류라 쿼리 전에 거른다.
        if (recipientIds.isEmpty()) {
            return List.of();
        }
        return notificationOutboxJpaRepository.findAllPendingByRecipientIdsForProcessing(recipientIds, limit);
    }

    @Override
    public List<NotificationOutbox> findStuckProcessingForUpdate(LocalDateTime threshold, int limit) {
        return notificationOutboxJpaRepository.findAllStuckForRecovery(
                OutboxStatus.PROCESSING, threshold, Pageable.ofSize(limit));
    }
}
