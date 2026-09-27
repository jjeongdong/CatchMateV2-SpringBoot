package com.back.catchmate.notification.infrastructure;

import static com.back.catchmate.notification.domain.QNotification.notification;

import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.NotificationRepository;
import com.back.catchmate.notification.domain.exception.NotificationNotFoundException;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
public class NotificationRepositoryImpl implements NotificationRepository {
    // created_at/modified_at 은 JPA 감사 기반이라 순수 JDBC INSERT 에선 자동으로 채워지지 않아 SQL 에서 직접 넣는다.
    // id 는 IDENTITY 라 컬럼에서 뺀다.
    private static final String BATCH_INSERT_SQL =
            """
            INSERT INTO notifications
                (user_id, sender_id, board_id, title, type, is_read, target_id, created_at, modified_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final NotificationJpaRepository notificationJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Notification save(Notification newNotification) {
        return notificationJpaRepository.save(newNotification);
    }

    @Override
    public Notification getById(Long notificationId) {
        return notificationJpaRepository.findById(notificationId).orElseThrow(NotificationNotFoundException::new);
    }

    @Override
    public void delete(Notification target) {
        notificationJpaRepository.delete(target);
    }

    // IDENTITY 라 Hibernate batch insert 가 불가능해 JdbcTemplate 로 적재한다.
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
                Notification row = notifications.get(i);
                ps.setLong(1, row.getUserId());
                setNullableLong(ps, 2, row.getSenderId());
                setNullableLong(ps, 3, row.getBoardId());
                ps.setString(4, row.getTitle());
                ps.setString(5, row.getType().name());
                ps.setBoolean(6, row.isRead());
                setNullableLong(ps, 7, row.getTargetId());
                ps.setTimestamp(8, now);
                ps.setTimestamp(9, now);
            }

            @Override
            public int getBatchSize() {
                return notifications.size();
            }
        });
    }

    // 정렬은 idx_notifications_user_created(user_id, created_at DESC) 와 짝을 이룬다.
    // 공지처럼 같은 시각에 적재된 행이 있어 id 로 순서를 가른다.
    @Override
    public List<Notification> findPageByUserId(Long userId, LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return jpaQueryFactory
                .selectFrom(notification)
                .where(notification.userId.eq(userId), olderThan(cursorCreatedAt, cursorId))
                .orderBy(notification.createdAt.desc(), notification.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public boolean existsUnreadByUserId(Long userId) {
        return notificationJpaRepository.existsByUserIdAndRead(userId, false);
    }

    @Override
    public int markAllReadByUserId(Long userId) {
        return notificationJpaRepository.markAllReadByUserId(userId);
    }

    private static BooleanExpression olderThan(LocalDateTime cursorCreatedAt, Long cursorId) {
        if (cursorCreatedAt == null || cursorId == null) {
            return null;
        }
        return notification
                .createdAt
                .lt(cursorCreatedAt)
                .or(notification.createdAt.eq(cursorCreatedAt).and(notification.id.lt(cursorId)));
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }
}
