package com.back.catchmate.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import com.back.catchmate.notification.domain.NotificationOutbox;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// 두 트랜잭션이 동시에 잠금을 잡아야 하므로 테스트 트랜잭션을 끄고 TransactionTemplate 으로 직접 연다.
// 빈 테이블에서는 옵티마이저가 status 선두 인덱스를 고르기 쉬워, 선점 쿼리가 필요 이상으로 잠그는지 드러난다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ImportAutoConfiguration(JdbcTemplateAutoConfiguration.class)
@Import({QuerydslConfig.class, JpaAuditingConfig.class, NotificationOutboxRepositoryImpl.class})
@Testcontainers(disabledWithoutDocker = true)
class NotificationOutboxClaimConcurrencyTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private NotificationOutboxRepositoryImpl notificationOutboxRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM notification_outbox");
    }

    @Test
    @DisplayName("여러 수신자 선점 - 겹치는 수신자를 동시에 선점해도 먼저 잡힌 행만 건너뛰어 한 행이 두 번 선점되지 않는다")
    void overlappingClaimsSkipOnlyLockedRows() throws Exception {
        // given — 수신자 1·2·3 에 대기 행 1건씩
        insertPending(1L, 2L, 3L);

        // when — 첫 트랜잭션이 1·2 를 잡고 있는 동안 두 번째 트랜잭션이 2·3 을 선점한다
        Claims claims = claimWhileFirstHolds(
                () -> notificationOutboxRepository.findPendingByRecipientIdsForUpdate(List.of(1L, 2L), 10),
                () -> notificationOutboxRepository.findPendingByRecipientIdsForUpdate(List.of(2L, 3L), 10));

        // then
        assertThat(recipientsOf(claims.first())).containsExactly(1L, 2L);
        assertThat(recipientsOf(claims.second())).containsExactly(3L);
    }

    @Test
    @DisplayName("여러 수신자 선점 - 한도만큼만 잠가 나머지 대기 행은 다른 선점이 가져갈 수 있다")
    void limitedClaimLocksOnlyLimitRows() throws Exception {
        // given — 수신자 1·2 에 대기 행 5건씩
        insertPending(1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L);

        // when — 첫 트랜잭션이 3건만 잡고 있는 동안 두 번째 트랜잭션이 전부를 선점하려 한다
        Claims claims = claimWhileFirstHolds(
                () -> notificationOutboxRepository.findPendingByRecipientIdsForUpdate(List.of(1L, 2L), 3),
                () -> notificationOutboxRepository.findPendingByRecipientIdsForUpdate(List.of(1L, 2L), 10));

        // then
        assertThat(claims.first()).hasSize(3);
        assertThat(claims.second()).hasSize(7);
    }

    @Test
    @DisplayName("단건 선점 - 한 수신자를 잡고 있어도 다른 수신자의 대기 행은 선점할 수 있다")
    void singleClaimLocksOnlyItsRecipient() throws Exception {
        // given — 수신자 1·2 에 대기 행 1건씩
        insertPending(1L, 2L);

        // when — 첫 트랜잭션이 수신자 1 을 잡고 있는 동안 두 번째 트랜잭션이 수신자 2 를 선점한다
        Claims claims = claimWhileFirstHolds(
                () -> notificationOutboxRepository.findPendingByRecipientIdForUpdate(1L),
                () -> notificationOutboxRepository.findPendingByRecipientIdForUpdate(2L));

        // then
        assertThat(recipientsOf(claims.first())).containsExactly(1L);
        assertThat(recipientsOf(claims.second())).containsExactly(2L);
    }

    private void insertPending(Long... recipientIds) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> List.of(recipientIds)
                .forEach(recipientId -> notificationOutboxRepository.save(
                        NotificationOutbox.create(recipientId, "t" + recipientId, "제목", "본문", "{}"))));
    }

    // 첫 선점 트랜잭션이 잠금을 쥔 채로 있는 동안 두 번째 선점을 실행한다.
    private Claims claimWhileFirstHolds(
            Supplier<List<NotificationOutbox>> firstClaim, Supplier<List<NotificationOutbox>> secondClaim)
            throws Exception {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch firstClaimed = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<List<NotificationOutbox>> first = executor.submit(() -> transactionTemplate.execute(status -> {
                List<NotificationOutbox> claimed = firstClaim.get();
                firstClaimed.countDown();
                awaitQuietly(releaseFirst);
                return claimed;
            }));
            assertThat(firstClaimed.await(10, TimeUnit.SECONDS)).isTrue();
            List<NotificationOutbox> second = transactionTemplate.execute(status -> secondClaim.get());
            releaseFirst.countDown();
            return new Claims(first.get(10, TimeUnit.SECONDS), second);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private static List<Long> recipientsOf(List<NotificationOutbox> outboxes) {
        return outboxes.stream().map(NotificationOutbox::getRecipientId).toList();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private record Claims(List<NotificationOutbox> first, List<NotificationOutbox> second) {}
}
