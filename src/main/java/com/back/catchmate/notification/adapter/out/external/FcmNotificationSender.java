package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.notification.application.port.out.external.NotificationSenderPort;
import com.back.catchmate.notification.application.port.out.exception.PermanentNotificationFailureException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmNotificationSender implements NotificationSenderPort {
    private final MeterRegistry meterRegistry;

    /**
     * [재시도 진입점]
     * 외부(OutboxDispatcher)에서 호출하는 메서드입니다.
     * 여기서 예외가 발생하면 설정된 backoff 시간만큼 대기 후 재시도합니다.
     */
    @Override
    @Retryable(
            retryFor = {RuntimeException.class},
            noRetryFor = {PermanentNotificationFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void sendNotification(Long userId, String token, String title, String body, Map<String, String> data) {
        Map<String, String> safeData = data != null ? data : Collections.emptyMap();

        log.info("FCM 발송 시도 - User: {}, Token: {}, Title: {}, Body: {}, Data: {}", userId, token, title, body, safeData);

        // Web Push 설정을 포함한 메시지 빌드
        WebpushConfig webpushConfig = WebpushConfig.builder()
                .setNotification(WebpushNotification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .setIcon("/catchmate-logo.svg")
                        .build())
                .build();

        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setWebpushConfig(webpushConfig)
                .putAllData(safeData)
                .setToken(token)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 메시지 전송 성공! Response ID: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 전송 실패 (FirebaseMessagingException) - Token: {}, ErrorCode: {}, Message: {}",
                    token, e.getMessagingErrorCode(), e.getMessage(), e);
            if (isPermanentFailure(e)) {
                log.error("FCM 영구 실패 발생 - errorCode: {}, token: {}", e.getMessagingErrorCode(), token);
                throw new PermanentNotificationFailureException(
                        "FCM 영구 실패 - errorCode: " + e.getMessagingErrorCode() + ", token: " + token, e);
            }
            log.warn("FCM 전송 실패 (재시도 예정) - errorCode: {}, 에러 메시지: {}, 토큰: {}",
                    e.getMessagingErrorCode(), e.getMessage(), token);
            throw new BaseException(ErrorCode.FCM_SEND_FAILED);
        } catch (Exception e) {
            log.error("FCM 전송 중 예상치 못한 에러 발생 - Token: {}, Message: {}", token, e.getMessage(), e);
            throw e;
        }
    }

    private boolean isPermanentFailure(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == null) return false;
        return switch (code) {
            case UNREGISTERED, INVALID_ARGUMENT, SENDER_ID_MISMATCH -> true;
            default -> false;
        };
    }

    /**
     * [복구 메서드]
     * 3번의 재시도(Retry)가 모두 실패했을 때 실행됩니다.
     */
    @Recover
    public void recover(RuntimeException e, Long userId, String token, String title, String body, Map<String, String> data) {
        log.error("FCM 푸시 전송 최종 실패 (User: {}) - {}", userId, e.getMessage());
        meterRegistry.counter("notification.fcm.send.failure", "type", "retry_exhausted").increment();
    }
}
