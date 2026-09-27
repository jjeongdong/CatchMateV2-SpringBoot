package com.back.catchmate.global.config.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulingConfig {

    // @Scheduled 작업 수(아웃박스 발송·회수·정리, 채팅 시퀀스 flush 2개)에 맞춰 서로 막지 않게 한다.
    private static final int POOL_SIZE = 5;

    // WebSocket 이 TaskScheduler 빈을 둘(wsHeartbeatScheduler, messageBrokerTaskScheduler) 만들어 Boot 의 기본 스케줄러가
    // 생기지 않고, @Scheduled 는 단일 스레드 폴백으로 떨어져 아웃박스 발송과 채팅 시퀀스 flush 가 서로를 기다렸다.
    // @Scheduled 는 이름이 taskScheduler 인 빈을 우선 쓰므로 이 이름으로 등록한다.
    @Bean(name = "taskScheduler")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(POOL_SIZE);
        scheduler.setThreadNamePrefix("Scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
