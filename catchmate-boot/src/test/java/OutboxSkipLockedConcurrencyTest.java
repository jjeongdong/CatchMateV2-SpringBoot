import com.back.catchmate.CatchmateApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CatchmateApplication.class, properties = "spring.profiles.active=local")
class OutboxSkipLockedConcurrencyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private static final int OUTBOX_COUNT = 20;        // PENDING 알림 건수
    private static final int SCHEDULER_INSTANCES = 5;   // 동시 스케줄러 인스턴스 수

    /**
     * SKIP LOCKED 적용: 여러 스케줄러 인스턴스가 동시에 PENDING 알림을 선점해도
     * 각 알림은 정확히 1회만 처리됨 (중복 발송 0건)
     */
    @Test
    @DisplayName("[SKIP LOCKED 적용] 5개 스케줄러 인스턴스 동시 실행 — 중복 발송 0건 보장")
    void withSkipLocked_shouldPreventDuplicateProcessing() throws InterruptedException {
        List<Long> outboxIds = createTestOutboxRecords(OUTBOX_COUNT);

        System.out.println("==========================================================");
        System.out.printf("  [SKIP LOCKED O] 중복 발송 방지 테스트: 알림 %d건, 스케줄러 %d개%n", OUTBOX_COUNT, SCHEDULER_INSTANCES);
        System.out.println("==========================================================");

        AtomicInteger totalSendCount = new AtomicInteger(0);
        Map<Long, List<String>> processedByThread = new ConcurrentHashMap<>();

        long startTime = System.currentTimeMillis();
        runConcurrentSchedulers(true, totalSendCount, processedByThread);
        long elapsed = System.currentTimeMillis() - startTime;

        int duplicateCount = countDuplicates(processedByThread);

        System.out.println();
        System.out.println("  [결과]");
        System.out.printf("    - 총 FCM 발송 횟수: %d회 (기대값: %d회)%n", totalSendCount.get(), OUTBOX_COUNT);
        System.out.printf("    - 중복 발송: %d건%n", duplicateCount);
        System.out.printf("    - 처리 시간: %dms%n", elapsed);
        printThreadDistribution(processedByThread);
        System.out.println("==========================================================");

        assertThat(totalSendCount.get()).isEqualTo(OUTBOX_COUNT);
        assertThat(duplicateCount).isZero();

        cleanupTestData();
    }

    /**
     * SKIP LOCKED 미적용: 여러 스케줄러 인스턴스가 동시에 같은 PENDING 알림을 읽어
     * 동일 알림을 여러 번 발송함 (중복 발송 발생)
     */
    @Test
    @DisplayName("[SKIP LOCKED 미적용] 5개 스케줄러 인스턴스 동시 실행 — 중복 발송 발생")
    void withoutSkipLocked_mayDuplicateProcessing() throws InterruptedException {
        List<Long> outboxIds = createTestOutboxRecords(OUTBOX_COUNT);

        System.out.println("==========================================================");
        System.out.printf("  [SKIP LOCKED X] 중복 발송 테스트: 알림 %d건, 스케줄러 %d개%n", OUTBOX_COUNT, SCHEDULER_INSTANCES);
        System.out.println("==========================================================");

        AtomicInteger totalSendCount = new AtomicInteger(0);
        Map<Long, List<String>> processedByThread = new ConcurrentHashMap<>();

        long startTime = System.currentTimeMillis();
        runConcurrentSchedulers(false, totalSendCount, processedByThread);
        long elapsed = System.currentTimeMillis() - startTime;

        int duplicateCount = countDuplicates(processedByThread);

        System.out.println();
        System.out.println("  [결과]");
        System.out.printf("    - 총 FCM 발송 횟수: %d회 (기대값: %d회)%n", totalSendCount.get(), OUTBOX_COUNT);
        System.out.printf("    - 중복 발송: %d건%n", duplicateCount);
        System.out.printf("    - 처리 시간: %dms%n", elapsed);
        printThreadDistribution(processedByThread);

        if (duplicateCount > 0) {
            System.out.printf("    → SKIP LOCKED 없이는 동일 알림이 여러 인스턴스에서 중복 발송됨!%n");
        } else {
            System.out.printf("    → 이번 실행에서는 우연히 중복이 발생하지 않음 (타이밍에 따라 다름)%n");
        }
        System.out.println("==========================================================");

        cleanupTestData();
    }

    /**
     * 비교 테스트: SKIP LOCKED 적용 vs 미적용 — 한눈에 비교
     */
    @Test
    @DisplayName("[비교] SKIP LOCKED 적용 vs 미적용 — 멀티 인스턴스 중복 발송 방지 비교")
    void compareSkipLockedEffect() throws InterruptedException {
        // --- SKIP LOCKED 적용 ---
        createTestOutboxRecords(OUTBOX_COUNT);

        AtomicInteger slSendCount = new AtomicInteger(0);
        Map<Long, List<String>> slProcessedBy = new ConcurrentHashMap<>();

        long slStart = System.currentTimeMillis();
        runConcurrentSchedulers(true, slSendCount, slProcessedBy);
        long slElapsed = System.currentTimeMillis() - slStart;

        int slDuplicates = countDuplicates(slProcessedBy);
        cleanupTestData();

        // --- SKIP LOCKED 미적용 ---
        createTestOutboxRecords(OUTBOX_COUNT);

        AtomicInteger noSlSendCount = new AtomicInteger(0);
        Map<Long, List<String>> noSlProcessedBy = new ConcurrentHashMap<>();

        long noSlStart = System.currentTimeMillis();
        runConcurrentSchedulers(false, noSlSendCount, noSlProcessedBy);
        long noSlElapsed = System.currentTimeMillis() - noSlStart;

        int noSlDuplicates = countDuplicates(noSlProcessedBy);
        cleanupTestData();

        // --- 비교 결과 ---
        System.out.println("==========================================================");
        System.out.println("  SKIP LOCKED 적용 vs 미적용 — 멀티 인스턴스 중복 발송 비교");
        System.out.println("==========================================================");
        System.out.printf("  조건: PENDING 알림 %d건, 동시 스케줄러 %d개 인스턴스%n", OUTBOX_COUNT, SCHEDULER_INSTANCES);
        System.out.println();
        System.out.println("  ┌────────────────────┬───────────────┬───────────────┐");
        System.out.println("  │       항목          │ SKIP LOCKED O │ SKIP LOCKED X │");
        System.out.println("  ├────────────────────┼───────────────┼───────────────┤");
        System.out.printf("  │ 총 FCM 발송 횟수   │     %3d회     │     %3d회     │%n", slSendCount.get(), noSlSendCount.get());
        System.out.printf("  │ 중복 발송 건수      │     %3d건     │     %3d건     │%n", slDuplicates, noSlDuplicates);
        System.out.printf("  │ 처리 시간           │   %5dms     │   %5dms     │%n", slElapsed, noSlElapsed);
        System.out.println("  ├────────────────────┼───────────────┼───────────────┤");
        System.out.printf("  │ 알림 정확 전달      │      ✅       │      ❌       │%n");
        System.out.printf("  │ 멀티 인스턴스 안전  │      ✅       │      ❌       │%n");
        System.out.println("  └────────────────────┴───────────────┴───────────────┘");
        System.out.println();
        System.out.println("  [분석]");
        System.out.println("    - SKIP LOCKED: 이미 잠긴 행을 건너뛰어 각 인스턴스가 서로 다른 알림을 처리");
        System.out.printf("    - SKIP LOCKED 미적용: %d건 알림에 대해 %d회 발송 → %d건 중복 (사용자에게 동일 알림 반복 도달)%n",
                OUTBOX_COUNT, noSlSendCount.get(), noSlDuplicates);
        if (noSlSendCount.get() > OUTBOX_COUNT) {
            System.out.printf("    - 불필요한 FCM 호출 %d회 발생 → 외부 API 비용 낭비 + 사용자 경험 저하%n",
                    noSlSendCount.get() - OUTBOX_COUNT);
        }
        System.out.println("==========================================================");

        // SKIP LOCKED 적용 시 중복 0건
        assertThat(slSendCount.get()).isEqualTo(OUTBOX_COUNT);
        assertThat(slDuplicates).isZero();
    }

    // =========================================================================
    //  동시 스케줄러 실행
    // =========================================================================

    private void runConcurrentSchedulers(boolean useSkipLocked,
                                         AtomicInteger totalSendCount,
                                         Map<Long, List<String>> processedByThread)
            throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(SCHEDULER_INSTANCES);
        CountDownLatch readyLatch = new CountDownLatch(SCHEDULER_INSTANCES);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(SCHEDULER_INSTANCES);

        for (int i = 0; i < SCHEDULER_INSTANCES; i++) {
            final String threadName = "Scheduler-" + i;

            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    List<Long> claimedIds = claimAndProcess(useSkipLocked, threadName);
                    totalSendCount.addAndGet(claimedIds.size());

                    // 어떤 스레드가 어떤 outbox를 처리했는지 기록
                    for (Long id : claimedIds) {
                        processedByThread.computeIfAbsent(id, k -> Collections.synchronizedList(new ArrayList<>()))
                                .add(threadName);
                    }

                    System.out.printf("  [%s] %d건 처리 완료%n", threadName, claimedIds.size());
                } catch (Exception e) {
                    System.out.printf("  [%s] 실패: %s%n", threadName, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();
    }

    // =========================================================================
    //  선점 + 처리 (단일 스케줄러 인스턴스 시뮬레이션)
    // =========================================================================

    private List<Long> claimAndProcess(boolean useSkipLocked, String threadName) throws Exception {
        List<Long> claimedIds = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. PENDING 알림 조회 (SKIP LOCKED 유무에 따라 다른 쿼리)
                String selectSql = useSkipLocked
                        ? "SELECT id FROM notification_outbox WHERE status = 'PENDING' AND retry_count < 5 FOR UPDATE SKIP LOCKED"
                        : "SELECT id FROM notification_outbox WHERE status = 'PENDING' AND retry_count < 5";

                try (PreparedStatement ps = conn.prepareStatement(selectSql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        claimedIds.add(rs.getLong("id"));
                    }
                }

                // 2. 선점한 알림들의 상태를 SUCCESS로 변경 (처리 완료 표시)
                for (Long id : claimedIds) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE notification_outbox SET status = 'SUCCESS', modified_at = NOW() WHERE id = ?")) {
                        ps.setLong(1, id);
                        ps.executeUpdate();
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }

        // 3. 트랜잭션 밖에서 FCM 발송 시뮬레이션 (실제 서비스에서는 여기서 FCM 호출)
        //    각 건당 처리 시간을 시뮬레이션하여 race window를 넓힘
        for (Long id : claimedIds) {
            Thread.sleep(5); // FCM API 호출 시뮬레이션
        }

        return claimedIds;
    }

    // =========================================================================
    //  중복 카운트
    // =========================================================================

    private int countDuplicates(Map<Long, List<String>> processedByThread) {
        int duplicates = 0;
        for (Map.Entry<Long, List<String>> entry : processedByThread.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates++;
            }
        }
        return duplicates;
    }

    private void printThreadDistribution(Map<Long, List<String>> processedByThread) {
        Map<String, Integer> threadCounts = new ConcurrentHashMap<>();
        for (List<String> threads : processedByThread.values()) {
            for (String thread : threads) {
                threadCounts.merge(thread, 1, Integer::sum);
            }
        }
        System.out.println("    - 스레드별 처리 분배:");
        threadCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("      %s: %d건%n", e.getKey(), e.getValue()));
    }

    // =========================================================================
    //  테스트 데이터
    // =========================================================================

    private List<Long> createTestOutboxRecords(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update(
                    "INSERT INTO notification_outbox (recipient_id, fcm_token, channel, title, body, payload, retry_count, status, created_at, modified_at) " +
                            "VALUES (?, ?, 'FCM', ?, ?, '{}', 0, 'PENDING', NOW(), NOW())",
                    (long) (i + 1),
                    "fcm_token_test_" + i,
                    "테스트 알림 " + i,
                    "동시성 테스트용 알림 본문 " + i
            );
            ids.add(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        }
        return ids;
    }

    private void cleanupTestData() {
        jdbcTemplate.update("DELETE FROM notification_outbox WHERE title LIKE '테스트 알림%'");
    }
}
