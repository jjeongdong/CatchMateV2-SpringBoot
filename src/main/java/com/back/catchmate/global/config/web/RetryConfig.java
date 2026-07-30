package com.back.catchmate.global.config.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 활성화 설정.
 *
 * <p>{@code @EnableRetry}가 있어야 {@code @Retryable} / {@code @Recover} 어노테이션의
 * AOP 인터셉터가 등록됩니다. 이 설정이 없으면 {@code EnrollAcceptExecutor}의 낙관적 락 충돌
 * 재시도가 무시(no-op)됩니다.
 *
 * <p>FCM 발송은 여기에 기대지 않습니다. 재시도를 아웃박스가 전담하므로
 * {@code FcmNotificationSender}에는 {@code @Retryable}을 두지 않습니다.
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
