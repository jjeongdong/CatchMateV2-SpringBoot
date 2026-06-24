package com.back.catchmate.global.config.web;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.executor.core-pool-size:50}")
    private int corePoolSize;

    @Value("${async.executor.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${async.executor.queue-capacity:5000}")
    private int queueCapacity;

    @Value("${notification.dispatch.executor.core-pool-size:50}")
    private int dispatchCorePoolSize;

    @Value("${notification.dispatch.executor.max-pool-size:50}")
    private int dispatchMaxPoolSize;

    @Value("${notification.dispatch.executor.queue-capacity:1000}")
    private int dispatchQueueCapacity;

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 1. 기본 스레드 수 (항상 유지되는 스레드)
        executor.setCorePoolSize(corePoolSize);

        // 2. 최대 스레드 수 (트래픽 폭증 시 확장)
        executor.setMaxPoolSize(maxPoolSize);

        // 3. 큐 용량 (스레드가 꽉 찼을 때 대기하는 작업 공간)
        executor.setQueueCapacity(queueCapacity);

        // 4. 스레드 이름 접두사 (모니터링 시 식별 용이)
        executor.setThreadNamePrefix("AsyncExecutor-");

        // 서버 종료 시 큐에 남은 작업을 마저 처리하고 종료하도록 대기함
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30); // 최대 30초 대기

        // 큐와 스레드가 모두 꽉 찼을 때, 호출한 스레드(메인)가 직접 처리하게 하여 누락 방지
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    /**
     * 알림 즉시발송(STOMP + FCM) 전용 executor.
     * <p>
     * 거부 정책으로 <b>CallerRunsPolicy 를 쓰지 않는다.</b> 즉시발송 리스너는
     * {@code @TransactionalEventListener(AFTER_COMMIT)} 로 동작하는데, 이 시점엔 방금 커밋한 트랜잭션의
     * 커넥션이 아직 반납되기 전이다. CallerRuns 로 웹 스레드에서 즉시발송을 실행하면 그 스레드가 기존 커넥션을 쥔 채
     * Outbox 갱신용 두 번째 커넥션을 요청해 풀 데드락을 유발한다.
     * <p>
     * 대신 과부하 시 즉시발송 작업을 <b>버린다</b>. 버려진 알림은 Outbox 에 PENDING 으로 남아
     * {@code NotificationScheduler}(60초 주기)가 회수하므로 유실되지 않는다(at-least-once).
     */
    @Bean(name = "notificationDispatchExecutor")
    public Executor notificationDispatchExecutor(MeterRegistry meterRegistry) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(dispatchCorePoolSize);
        executor.setMaxPoolSize(dispatchMaxPoolSize);
        executor.setQueueCapacity(dispatchQueueCapacity);
        executor.setThreadNamePrefix("NotiDispatch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setRejectedExecutionHandler((rejected, exec) -> {
            // 즉시발송 포기 — 스케줄러가 PENDING Outbox 를 회수한다. 웹 스레드 데드락 방지를 위해 절대 CallerRuns 하지 않는다.
            meterRegistry.counter("notification.dispatch.shed").increment();
            if (log.isDebugEnabled()) {
                log.debug("[알림] 즉시발송 executor 포화 — 즉시발송 건너뜀(스케줄러가 회수). queueSize={}", exec.getQueue().size());
            }
        });

        executor.initialize();
        return executor;
    }
}
