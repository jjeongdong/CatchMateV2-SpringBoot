package com.back.catchmate.notification.repository;

import com.back.catchmate.notification.entity.Notification;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {
    // created_at/modified_at 은 JPA 감사(@CreatedDate/@LastModifiedDate) 기반이라 순수 JDBC INSERT 에선
    // 자동 세팅되지 않으므로 SQL 에서 직접 채운다. id 는 IDENTITY 라 컬럼에서 제외한다.
    private static final String BATCH_INSERT_SQL =
            """
            INSERT INTO notifications
                (user_id, sender_id, board_id, title, type, is_read, target_id, created_at, modified_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    // 수신자 수만큼의 알림을 단일 멀티로우 INSERT 로 적재한다(IDENTITY 라 Hibernate batch 불가 → JdbcTemplate).
    // JDBC URL 의 rewriteBatchedStatements=true 가 있어야 한 번의 왕복으로 재작성된다.
    @Override
    public void saveAllInBatch(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.batchUpdate(BATCH_INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Notification notification = notifications.get(i);
                ps.setLong(1, notification.getUserId());
                setNullableLong(ps, 2, notification.getSenderId());
                setNullableLong(ps, 3, notification.getBoardId());
                ps.setString(4, notification.getTitle());
                ps.setString(5, notification.getType().name());
                ps.setBoolean(6, notification.isRead());
                setNullableLong(ps, 7, notification.getTargetId());
                ps.setTimestamp(8, now);
                ps.setTimestamp(9, now);
            }

            @Override
            public int getBatchSize() {
                return notifications.size();
            }
        });
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }
}
