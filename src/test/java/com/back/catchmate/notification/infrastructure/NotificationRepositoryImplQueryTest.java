package com.back.catchmate.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import com.back.catchmate.notification.domain.AlarmType;
import com.back.catchmate.notification.domain.Notification;
import com.back.catchmate.notification.domain.NotificationOutbox;
import com.back.catchmate.notification.domain.OutboxStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// 운영 RDS 를 가리키는 dev 프로필을 끄고 컨테이너 DB 에만 붙는다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ImportAutoConfiguration(JdbcTemplateAutoConfiguration.class)
@Import({
    QuerydslConfig.class,
    JpaAuditingConfig.class,
    NotificationRepositoryImpl.class,
    NotificationOutboxRepositoryImpl.class
})
@Testcontainers(disabledWithoutDocker = true)
class NotificationRepositoryImplQueryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private NotificationRepositoryImpl notificationRepository;

    @Autowired
    private NotificationOutboxRepositoryImpl notificationOutboxRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final LocalDateTime FINISHED_LONG_AGO = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final LocalDateTime CLEANUP_THRESHOLD = LocalDateTime.of(2026, 1, 10, 12, 0);
    private static final LocalDateTime FINISHED_RECENTLY = LocalDateTime.of(2026, 1, 20, 12, 0);

    @Test
    @DisplayName("한 번에 적재돼 created_at 이 같은 알림도 커서가 id 로 이어 받아 빠짐·중복이 없다")
    void cursorBreaksTiesById() {
        // given — 공지처럼 멀티로우 INSERT 로 같은 시각에 3건 + 다른 사용자 1건
        notificationRepository.saveAllInBatch(List.of(
                Notification.create(1L, null, null, "a", AlarmType.EVENT, 1L),
                Notification.create(1L, null, null, "b", AlarmType.EVENT, 1L),
                Notification.create(1L, null, null, "c", AlarmType.EVENT, 1L),
                Notification.create(2L, null, null, "x", AlarmType.EVENT, 1L)));

        // when
        List<Notification> first = notificationRepository.findPageByUserId(1L, null, null, 2);
        Notification last = first.get(1);
        List<Notification> second = notificationRepository.findPageByUserId(1L, last.getCreatedAt(), last.getId(), 2);

        // then
        assertThat(first).extracting(Notification::getTitle).containsExactly("c", "b");
        assertThat(second).extracting(Notification::getTitle).containsExactly("a");
    }

    @Test
    @DisplayName("안 읽은 알림 여부와 전체 읽음 처리")
    void unreadAndMarkAll() {
        notificationRepository.save(Notification.create(1L, null, null, "a", AlarmType.EVENT, 1L));
        notificationRepository.save(Notification.create(1L, null, null, "b", AlarmType.EVENT, 1L));

        assertThat(notificationRepository.existsUnreadByUserId(1L)).isTrue();
        assertThat(notificationRepository.markAllReadByUserId(1L)).isEqualTo(2);
        assertThat(notificationRepository.existsUnreadByUserId(1L)).isFalse();
        assertThat(notificationRepository.existsUnreadByUserId(2L)).isFalse();
    }

    @Test
    @DisplayName("선점 후 JDBC 로 반영한 행은 영속성 컨텍스트에서 떼어 커밋 때 다시 UPDATE 되지 않게 한다")
    void updateAllDetaches() {
        // given
        notificationOutboxRepository.save(NotificationOutbox.create(1L, "t", "제목", "본문", "{}"));
        entityManager.flush();
        entityManager.clear();
        List<NotificationOutbox> pending = notificationOutboxRepository.findPendingForUpdate(5, 10);
        pending.forEach(NotificationOutbox::startProcessing);

        // when
        notificationOutboxRepository.updateAll(pending);

        // then
        assertThat(pending).hasSize(1).allSatisfy(outbox -> assertThat(entityManager.contains(outbox))
                .isFalse());
        assertThat(notificationOutboxRepository.findPendingForUpdate(5, 10)).isEmpty();
        assertThat(entityManager
                        .find(NotificationOutbox.class, pending.get(0).getId())
                        .getStatus())
                .isEqualTo(OutboxStatus.PROCESSING);
    }

    @Test
    @DisplayName("아웃박스 선점 - 여러 수신자의 대기 행만 수신자·id 순으로 조회한다")
    void findsPendingByRecipientIds() {
        // given — 1·2 의 대기 행, 다른 수신자 3 의 대기 행, 1 의 처리 중 행
        notificationOutboxRepository.save(NotificationOutbox.create(1L, "t1", "제목", "본문", "{}"));
        notificationOutboxRepository.save(NotificationOutbox.create(2L, "t2", "제목", "본문", "{}"));
        notificationOutboxRepository.save(NotificationOutbox.create(3L, "t3", "제목", "본문", "{}"));
        NotificationOutbox processing = NotificationOutbox.create(1L, "t1", "제목", "본문", "{}");
        processing.startProcessing();
        notificationOutboxRepository.save(processing);
        entityManager.flush();
        entityManager.clear();

        // when
        List<NotificationOutbox> found =
                notificationOutboxRepository.findPendingByRecipientIdsForUpdate(List.of(1L, 2L), 10);

        // then
        assertThat(found).extracting(NotificationOutbox::getRecipientId).containsExactly(1L, 2L);
        assertThat(found).extracting(NotificationOutbox::getStatus).containsOnly(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("아웃박스 선점 - 수신자가 없으면 빈 목록을 돌려준다")
    void findsNothingForEmptyRecipients() {
        // when
        List<NotificationOutbox> found = notificationOutboxRepository.findPendingByRecipientIdsForUpdate(List.of(), 10);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("아웃박스 선점 - 한도까지만 수신자 순, 그 안에서 오래된 행부터 조회한다")
    void findsPendingByRecipientIdsUpToLimit() {
        // given — 밀린 대기 행 3건
        NotificationOutbox oldest =
                notificationOutboxRepository.save(NotificationOutbox.create(1L, "t1", "제목", "본문", "{}"));
        NotificationOutbox middle =
                notificationOutboxRepository.save(NotificationOutbox.create(1L, "t1", "제목", "본문", "{}"));
        notificationOutboxRepository.save(NotificationOutbox.create(2L, "t2", "제목", "본문", "{}"));
        entityManager.flush();
        entityManager.clear();

        // when
        List<NotificationOutbox> found =
                notificationOutboxRepository.findPendingByRecipientIdsForUpdate(List.of(1L, 2L), 2);

        // then
        assertThat(found).extracting(NotificationOutbox::getId).containsExactly(oldest.getId(), middle.getId());
    }

    @Test
    @DisplayName("아웃박스 정리 - 지정한 상태 중 기준 시각 이전에 끝난 행만 지운다")
    void deletesFinishedBefore() {
        // given
        saveOutbox(OutboxStatus.SUCCESS, FINISHED_LONG_AGO);
        Long recentSuccess = saveOutbox(OutboxStatus.SUCCESS, FINISHED_RECENTLY);
        Long oldFailed = saveOutbox(OutboxStatus.FAILED, FINISHED_LONG_AGO);
        Long oldPending = saveOutbox(OutboxStatus.PENDING, FINISHED_LONG_AGO);
        Long oldProcessing = saveOutbox(OutboxStatus.PROCESSING, FINISHED_LONG_AGO);

        // when
        int deleted =
                notificationOutboxRepository.deleteFinishedBefore(List.of(OutboxStatus.SUCCESS), CLEANUP_THRESHOLD, 10);

        // then
        assertThat(deleted).isEqualTo(1);
        assertThat(remainingOutboxIds()).containsExactlyInAnyOrder(recentSuccess, oldFailed, oldPending, oldProcessing);
    }

    @Test
    @DisplayName("아웃박스 정리 - 한 번에 한도만큼만 지운다")
    void deletesFinishedBeforeUpToLimit() {
        // given
        saveOutbox(OutboxStatus.SUCCESS, FINISHED_LONG_AGO);
        saveOutbox(OutboxStatus.SUCCESS, FINISHED_LONG_AGO);
        saveOutbox(OutboxStatus.SUCCESS, FINISHED_LONG_AGO);

        // when
        int deleted =
                notificationOutboxRepository.deleteFinishedBefore(List.of(OutboxStatus.SUCCESS), CLEANUP_THRESHOLD, 2);

        // then
        assertThat(deleted).isEqualTo(2);
        assertThat(remainingOutboxIds()).hasSize(1);
    }

    // 상태·수정 시각은 도메인 전이와 JPA 감사가 정하므로, 과거 시점을 만들려고 저장 뒤 JDBC 로 덮어쓴다.
    private Long saveOutbox(OutboxStatus status, LocalDateTime modifiedAt) {
        NotificationOutbox outbox =
                notificationOutboxRepository.save(NotificationOutbox.create(1L, "t", "제목", "본문", "{}"));
        entityManager.flush();
        entityManager.clear();
        jdbcTemplate.update(
                "UPDATE notification_outbox SET status = ?, modified_at = ? WHERE id = ?",
                status.name(),
                modifiedAt,
                outbox.getId());
        return outbox.getId();
    }

    private List<Long> remainingOutboxIds() {
        return jdbcTemplate.queryForList("SELECT id FROM notification_outbox", Long.class);
    }
}
