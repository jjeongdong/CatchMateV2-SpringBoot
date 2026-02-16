package com.back.catchmate.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 1. 기본 스레드 수 (항상 유지되는 스레드)
        executor.setCorePoolSize(10);

        // 2. 최대 스레드 수 (트래픽 폭증 시 확장)
        executor.setMaxPoolSize(50);

        // 3. 큐 용량 (스레드가 꽉 찼을 때 대기하는 작업 공간)
        executor.setQueueCapacity(100);

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
}
