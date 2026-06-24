package com.back.catchmate.global.config.web;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer 메트릭 보조 설정.
 *
 * <p>{@link TimedAspect} 빈을 등록해 임의의 Spring 빈 메서드에 {@code @Timed} 를 붙여
 * 실행 시간을 Timer 로 수집할 수 있게 한다. HTTP 진입점은 actuator 가 자동으로 수집하므로
 * 주로 비동기 알림 발송(스케줄러/Outbox) 등 HTTP 가 아닌 경로의 처리 시간 측정에 사용한다.
 */
@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }
}
