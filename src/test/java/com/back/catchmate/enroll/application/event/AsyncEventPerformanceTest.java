package com.back.catchmate.enroll.application.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 외부 알림 API(FCM) 지연이 메인 트랜잭션 응답 시간에 미치는 영향을 측정하는 테스트.
 *
 * 시뮬레이션 조건:
 * - 메인 비즈니스 로직: 50ms (DB 쿼리 + 로직)
 * - FCM 외부 API 호출: 200~500ms (네트워크 지연 시뮬레이션)
 *
 * Before(동기): 비즈니스 로직 + FCM 호출이 직렬 실행 → 응답에 FCM 지연 포함
 * After(비동기): FCM 호출은 @Async로 분리 → 응답에 FCM 지연 미포함
 */
class AsyncEventPerformanceTest {

    private static final int BUSINESS_LOGIC_MS = 50;
    private static final int FCM_MIN_DELAY_MS = 200;
    private static final int FCM_MAX_DELAY_MS = 500;
    private static final int ITERATION_COUNT = 20;

    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(10);

    /**
     * FCM 외부 API 호출을 시뮬레이션 (200~500ms 지연)
     */
    private void simulateFcmCall() {
        try {
            int delay = FCM_MIN_DELAY_MS + (int) (Math.random() * (FCM_MAX_DELAY_MS - FCM_MIN_DELAY_MS));
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 메인 비즈니스 로직을 시뮬레이션 (DB 쿼리 등)
     */
    private void simulateBusinessLogic() {
        try {
            Thread.sleep(BUSINESS_LOGIC_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Before: 동기 방식 — FCM 호출이 메인 스레드에서 직렬 실행
     */
    private long measureSyncResponse() {
        long start = System.currentTimeMillis();

        // 1. 비즈니스 로직 (트랜잭션 내)
        simulateBusinessLogic();

        // 2. 알림 발송 (동기 — 메인 스레드에서 실행)
        simulateFcmCall();

        return System.currentTimeMillis() - start;
    }

    /**
     * After: 비동기 방식 — FCM 호출은 별도 스레드에서 실행
     */
    private long measureAsyncResponse() {
        long start = System.currentTimeMillis();

        // 1. 비즈니스 로직 (트랜잭션 내)
        simulateBusinessLogic();

        // 2. 알림 발송 (@Async — 별도 스레드에서 실행, 응답에 영향 없음)
        CompletableFuture.runAsync(this::simulateFcmCall, asyncExecutor);

        return System.currentTimeMillis() - start;
    }

    @Test
    @DisplayName("동기 vs 비동기 알림 발송 — API 응답 시간 비교")
    void compareResponseTime() {
        // Warm-up
        for (int i = 0; i < 5; i++) {
            measureSyncResponse();
            measureAsyncResponse();
        }

        List<Long> syncTimes = new ArrayList<>();
        List<Long> asyncTimes = new ArrayList<>();

        for (int i = 0; i < ITERATION_COUNT; i++) {
            syncTimes.add(measureSyncResponse());
            asyncTimes.add(measureAsyncResponse());
        }

        double syncAvg = syncTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double asyncAvg = asyncTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long syncMax = syncTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long asyncMax = asyncTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        double improvement = ((syncAvg - asyncAvg) / syncAvg) * 100;

        System.out.println("==========================================================");
        System.out.println("  외부 알림 API 동기/비동기 분리 — 응답 시간 측정 결과");
        System.out.println("==========================================================");
        System.out.println();
        System.out.printf("  시뮬레이션 조건:%n");
        System.out.printf("    - 비즈니스 로직: %dms%n", BUSINESS_LOGIC_MS);
        System.out.printf("    - FCM 외부 API: %d~%dms%n", FCM_MIN_DELAY_MS, FCM_MAX_DELAY_MS);
        System.out.printf("    - 반복 횟수: %d회%n", ITERATION_COUNT);
        System.out.println();
        System.out.printf("  [Before] 동기 방식 (FCM 호출이 응답에 포함)%n");
        System.out.printf("    - 평균 응답 시간: %.1fms%n", syncAvg);
        System.out.printf("    - 최대 응답 시간: %dms%n", syncMax);
        System.out.println();
        System.out.printf("  [After] 비동기 방식 (@Async + @TransactionalEventListener)%n");
        System.out.printf("    - 평균 응답 시간: %.1fms%n", asyncAvg);
        System.out.printf("    - 최대 응답 시간: %dms%n", asyncMax);
        System.out.println();
        System.out.printf("  개선율: %.1f%% (%.1fms → %.1fms)%n", improvement, syncAvg, asyncAvg);
        System.out.println("==========================================================");

        // 비동기 방식이 동기 방식보다 최소 50% 이상 빨라야 함
        assertThat(asyncAvg).isLessThan(syncAvg * 0.5);
        // 비동기 응답 시간은 비즈니스 로직 시간 + 여유분(30ms)을 넘지 않아야 함
        assertThat(asyncAvg).isLessThan(BUSINESS_LOGIC_MS + 30);
    }
}
