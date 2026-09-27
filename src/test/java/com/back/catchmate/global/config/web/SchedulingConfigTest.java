package com.back.catchmate.global.config.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig({SchedulingConfig.class, SchedulingConfigTest.TestConfig.class})
class SchedulingConfigTest {

    @Autowired
    private ScheduledProbe probe;

    @Test
    @DisplayName("다른 TaskScheduler 빈이 있어도 @Scheduled 는 taskScheduler 풀에서 실행된다")
    void runsScheduledTasksOnTaskScheduler() throws Exception {
        // when
        String threadName = probe.executedThreadName.get(5, TimeUnit.SECONDS);

        // then
        assertThat(threadName).startsWith("Scheduler-");
    }

    @Configuration
    @EnableScheduling
    static class TestConfig {

        // WebSocket 설정처럼 TaskScheduler 빈이 여럿인 상황을 재현한다.
        @Bean
        TaskScheduler otherScheduler1() {
            return scheduler("Other1-");
        }

        @Bean
        TaskScheduler otherScheduler2() {
            return scheduler("Other2-");
        }

        @Bean
        ScheduledProbe scheduledProbe() {
            return new ScheduledProbe();
        }

        private static ThreadPoolTaskScheduler scheduler(String prefix) {
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setThreadNamePrefix(prefix);
            scheduler.initialize();
            return scheduler;
        }
    }

    static class ScheduledProbe {
        private final CompletableFuture<String> executedThreadName = new CompletableFuture<>();

        @Scheduled(fixedDelay = 100)
        void record() {
            executedThreadName.complete(Thread.currentThread().getName());
        }
    }
}
