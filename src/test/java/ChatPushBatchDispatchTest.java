import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.CatchmateApplication;
import com.back.catchmate.notification.application.OutboxDispatcher;
import com.back.catchmate.notification.domain.PushMessage;
import com.back.catchmate.notification.domain.PushOutcome;
import com.back.catchmate.notification.domain.PushSender;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

// 채팅 푸시를 수신자별 단건으로 보내던 옛 경로와 배치 경로를 같은 빌드에서 비교한다.
// 고정 지연과 로컬 MySQL·Redis 에 기대는 벤치라 기본 빌드에서 빼고 ./gradlew benchTest 로만 돈다.
// FCM 은 가짜로 바꿔 호출 1회당 고정 지연을 준다. sendEach 는 SDK 내부에서 병렬 발사되므로 1회 지연으로 근사한다.
@Tag("bench")
@SpringBootTest(
        classes = CatchmateApplication.class,
        properties = {"spring.profiles.active=local", "spring.jpa.properties.hibernate.generate_statistics=true"})
@Import(ChatPushBatchDispatchTest.LatencyPushSenderConfig.class)
class ChatPushBatchDispatchTest {

    private static final Logger log = LoggerFactory.getLogger(ChatPushBatchDispatchTest.class);

    private static final long FCM_LATENCY_MS = 100;
    private static final int REPEAT = 5;
    // 로컬 DB 의 실제 데이터와 섞이지 않도록 쓰지 않는 수신자 ID 대역을 쓴다. 정리 때 이 대역의 행은 모두 지운다.
    private static final long RECIPIENT_BASE = 990_000L;
    private static final String CHAT_PAYLOAD = "{\"type\":\"CHAT\",\"roomId\":\"990000\"}";

    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @Autowired
    private LatencyPushSender pushSender;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM notification_outbox WHERE recipient_id >= ?", RECIPIENT_BASE);
    }

    @ParameterizedTest(name = "수신자 {0}명")
    @ValueSource(ints = {2, 4, 8})
    @DisplayName("배치 발송은 수신자 수와 무관하게 FCM 1회·트랜잭션 2회로 보낸다")
    void comparesSingleAndBatchDispatch(int recipientCount) {
        // given
        List<Long> recipientIds = LongStream.range(RECIPIENT_BASE, RECIPIENT_BASE + recipientCount)
                .boxed()
                .toList();

        // when
        Measurement single =
                measure(recipientIds, () -> recipientIds.forEach(outboxDispatcher::sendPendingOutboxImmediately));
        Measurement batch = measure(recipientIds, () -> outboxDispatcher.sendPendingOutboxesImmediately(recipientIds));

        // then
        log.info(
                "[채팅 푸시 벤치] 수신자={} 단건: 평균 {}ms, FCM {}회, 트랜잭션 {}회 | 배치: 평균 {}ms, FCM {}회, 트랜잭션 {}회",
                recipientCount,
                single.averageMillis(),
                single.fcmCalls(),
                single.transactions(),
                batch.averageMillis(),
                batch.fcmCalls(),
                batch.transactions());
        assertThat(single.fcmCalls()).isEqualTo(recipientCount);
        assertThat(batch.fcmCalls()).isEqualTo(1);
        // 트랜잭션 수는 JVM 전역 통계라 다른 스케줄러가 끼어들 수 있어 정확한 값 대신 대소만 본다.
        assertThat(batch.transactions()).isLessThan(single.transactions());
    }

    private Measurement measure(List<Long> recipientIds, Runnable dispatch) {
        Statistics statistics =
                entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        long totalNanos = 0;
        int fcmCalls = 0;
        long transactions = 0;
        for (int i = 0; i < REPEAT; i++) {
            insertPending(recipientIds);
            pushSender.reset();
            long transactionsBefore = statistics.getTransactionCount();
            long start = System.nanoTime();
            dispatch.run();
            totalNanos += System.nanoTime() - start;
            fcmCalls = pushSender.calls();
            transactions = statistics.getTransactionCount() - transactionsBefore;
            assertThat(countNotSucceeded()).isZero();
            cleanUp();
        }
        return new Measurement(TimeUnit.NANOSECONDS.toMillis(totalNanos / REPEAT), fcmCalls, transactions);
    }

    private void insertPending(List<Long> recipientIds) {
        for (Long recipientId : recipientIds) {
            jdbcTemplate.update(
                    """
                    INSERT INTO notification_outbox
                        (recipient_id, fcm_token, title, body, payload,
                         retry_count, status, error_message, created_at, modified_at)
                    VALUES (?, ?, '제목', '본문', ?, 0, 'PENDING', NULL, NOW(), NOW())
                    """,
                    recipientId,
                    "token-" + recipientId,
                    CHAT_PAYLOAD);
        }
    }

    private long countNotSucceeded() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_outbox WHERE recipient_id >= ? AND status <> 'SUCCESS'",
                Long.class,
                RECIPIENT_BASE);
        return count != null ? count : 0L;
    }

    private record Measurement(long averageMillis, int fcmCalls, long transactions) {}

    @TestConfiguration
    static class LatencyPushSenderConfig {
        @Bean
        @Primary
        LatencyPushSender latencyPushSender() {
            return new LatencyPushSender();
        }
    }

    static class LatencyPushSender implements PushSender {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public PushOutcome send(PushMessage message) {
            simulateFcmCall();
            return PushOutcome.ofSuccess();
        }

        @Override
        public List<PushOutcome> sendAll(List<PushMessage> messages) {
            simulateFcmCall();
            return messages.stream().map(message -> PushOutcome.ofSuccess()).toList();
        }

        int calls() {
            return calls.get();
        }

        void reset() {
            calls.set(0);
        }

        private void simulateFcmCall() {
            calls.incrementAndGet();
            try {
                Thread.sleep(FCM_LATENCY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }
}
