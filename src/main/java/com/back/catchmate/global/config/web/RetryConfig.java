package com.back.catchmate.global.config.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 활성화 설정.
 *
 * <p>{@code @EnableRetry}가 있어야 {@code @Retryable} / {@code @Recover} 어노테이션의
 * AOP 인터셉터가 등록됩니다. 이 설정이 없으면 {@code FcmNotificationSender}의
 * {@code @Retryable} 재시도가 무시(no-op)됩니다.
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
