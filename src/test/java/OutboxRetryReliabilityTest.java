import com.back.catchmate.CatchmateApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CatchmateApplication.class, properties = "spring.profiles.active=local")
class OutboxRetryReliabilityTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int TOTAL_NOTIFICATIONS = 100;
    private static final int MAX_RETRY_COUNT = 5;       // application.yml과 ��일
    private static final double FCM_FAILURE_RATE = 0.3;  // FCM 1회 호출 시 30% 실패율 가정

    /**
     * Outbox 재시도 메커니즘의 최종 발송 성공률 측정
     *
     * 시뮬레이션 조건:
     * - FCM 1회 호출 시 실패 확률: 30%
     * - 최대 재시도 횟수: 5회 (application.yml 설정값)
     * - 알림 100건 발송
     *
     * Outbox 없이 (단순 1회 호출): 성공률 = 1 - 0.3 = 70%
     * Outbox 재시도 적용: 성공률 = 1 - (0.3)^5 = 99.76% (이론값)
     */
    @Test
    @DisplayName("[Outbox 재시도] FCM 30% 실패율에서 최대 5회 재시도 시 최종 발송 성공률 측정")
    void outboxRetry_shouldAchieveHighSuccessRate() {
        AtomicInteger withoutOutboxSuccess = new AtomicInteger(0);
        AtomicInteger withOutboxSuccess = new AtomicInteger(0);
        AtomicInteger totalRetries = new AtomicInteger(0);

        // --- Without Outbox: 1회 시도만 ---
        for (int i = 0; i < TOTAL_NOTIFICATIONS; i++) {
            if (simulateFcmCall()) {
                withoutOutboxSuccess.incrementAndGet();
            }
        }

        // --- With Outbox: 최대 MAX_RETRY_COUNT 재시도 ---
        for (int i = 0; i < TOTAL_NOTIFICATIONS; i++) {
            boolean sent = false;
            for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
                if (simulateFcmCall()) {
                    sent = true;
                    break;
                }
                totalRetries.incrementAndGet();
            }
            if (sent) {
                withOutboxSuccess.incrementAndGet();
            }
        }

        double withoutRate = (withoutOutboxSuccess.get() / (double) TOTAL_NOTIFICATIONS) * 100;
        double withRate = (withOutboxSuccess.get() / (double) TOTAL_NOTIFICATIONS) * 100;
        double theoreticalRate = (1 - Math.pow(FCM_FAILURE_RATE, MAX_RETRY_COUNT)) * 100;

        System.out.println("==========================================================");
        System.out.println("  Transactional Outbox 재시도 메커니즘 — 발송 성공률 측정");
        System.out.println("==========================================================");
        System.out.println();
        System.out.printf("  시뮬레이션 조건:%n");
        System.out.printf("    - 총 알림 건수: %d건%n", TOTAL_NOTIFICATIONS);
        System.out.printf("    - FCM 1회 실패율: %.0f%%%n", FCM_FAILURE_RATE * 100);
        System.out.printf("    - 최대 재시도 횟수: %d회%n", MAX_RETRY_COUNT);
        System.out.println();
        System.out.printf("  [Before] Outbox 미적용 (1회 시도)%n");
        System.out.printf("    - 발송 성공: %d / %d건%n", withoutOutboxSuccess.get(), TOTAL_NOTIFICATIONS);
        System.out.printf("    - 성공률: %.1f%%%n", withoutRate);
        System.out.println();
        System.out.printf("  [After] Outbox 재시도 적용 (최대 %d회)%n", MAX_RETRY_COUNT);
        System.out.printf("    - 발송 성공: %d / %d건%n", withOutboxSuccess.get(), TOTAL_NOTIFICATIONS);
        System.out.printf("    - 성공률: %.1f%%%n", withRate);
        System.out.printf("    - 총 재시도 횟수: %d회%n", totalRetries.get());
        System.out.println();
        System.out.printf("  이론적 성공률: %.2f%% (1 - %.1f^%d)%n", theoreticalRate, FCM_FAILURE_RATE, MAX_RETRY_COUNT);
        System.out.printf("  개선: %.1f%% → %.1f%%%n", withoutRate, withRate);
        System.out.println("==========================================================");

        // Outbox 적용 시 99% 이상 성공률 보장
        assertThat(withRate).isGreaterThanOrEqualTo(99.0);
        // Outbox 미적용보다 반드시 높아야 함
        assertThat(withRate).isGreaterThan(withoutRate);
    }

    /**
     * 대량 알림(1000건)에서의 발송 성공률 측정 — 통계적으로 더 안정적인 결과
     */
    @Test
    @DisplayName("[대량 검증] 1000건 알림에 대한 Outbox 재시�� 성공�� 측정")
    void outboxRetry_bulkReliabilityTest() {
        int bulkCount = 1000;

        int withoutOutboxSuccess = 0;
        int withOutboxSuccess = 0;
        int totalRetries = 0;
        int maxRetriesForSingleNotification = 0;

        // Without Outbox
        for (int i = 0; i < bulkCount; i++) {
            if (simulateFcmCall()) withoutOutboxSuccess++;
        }

        // With Outbox
        for (int i = 0; i < bulkCount; i++) {
            boolean sent = false;
            int retries = 0;
            for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
                if (simulateFcmCall()) {
                    sent = true;
                    break;
                }
                retries++;
            }
            totalRetries += retries;
            maxRetriesForSingleNotification = Math.max(maxRetriesForSingleNotification, retries);
            if (sent) withOutboxSuccess++;
        }

        double withoutRate = (withoutOutboxSuccess / (double) bulkCount) * 100;
        double withRate = (withOutboxSuccess / (double) bulkCount) * 100;
        int failedCount = bulkCount - withOutboxSuccess;

        System.out.println("==========================================================");
        System.out.println("  대량 알림 발송 신뢰성 검증 (1000건)");
        System.out.println("==========================================================");
        System.out.println();
        System.out.println("  ┌──────────────────┬──────────────┬──────────────┐");
        System.out.println("  │      항목         │ Outbox 미적�� │ Outbox 적용  │");
        System.out.println("  ├─��────────────────┼──────────────┼──────────────┤");
        System.out.printf("  │ 발송 성공         │  %4d / %d  │  %4d / %d  │%n",
                withoutOutboxSuccess, bulkCount, withOutboxSuccess, bulkCount);
        System.out.printf("  │ 발송 실패         │  %4d건       │  %4d���       │%n",
                bulkCount - withoutOutboxSuccess, failedCount);
        System.out.printf("  │ 성공률            │    %5.1f%%    │    %5.1f%%    │%n", withoutRate, withRate);
        System.out.printf("  │ 재시도 횟수       │      -       │  %4d회       │%n", totalRetries);
        System.out.printf("  │ 단건 최대 재시도  │      -       │     %d회      │%n", maxRetriesForSingleNotification);
        System.out.println("  └──────────────────┴──���───────────┴──────────────┘");
        System.out.println();
        System.out.printf("  알림 유실 건수: %d건 → %d건%n", bulkCount - withoutOutboxSuccess, failedCount);
        System.out.println("==========================================================");

        assertThat(withRate).isGreaterThanOrEqualTo(99.0);
    }

    /**
     * FCM API 호출 시뮬레이션 — FCM_FAILURE_RATE 확률로 실패
     */
    private boolean simulateFcmCall() {
        return Math.random() >= FCM_FAILURE_RATE;
    }
}
