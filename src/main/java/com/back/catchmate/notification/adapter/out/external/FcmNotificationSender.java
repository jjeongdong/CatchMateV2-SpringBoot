package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.notification.application.port.out.external.NotificationSenderPort;
import com.back.catchmate.notification.application.port.out.exception.PermanentNotificationFailureException;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
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

        log.debug("FCM 발송 시도 - User: {}, Token: {}, Title: {}, Body: {}, Data: {}", userId, token, title, body, safeData);

        // 같은 outbox 행의 재발송(FCM 타임아웃 재시도·PROCESSING 회수)이면 tag 가 동일하므로
        // OS 가 배너를 새로 쌓지 않고 기존 것을 교체한다. 수신 측이 아무 처리를 하지 않아도 중복이 보이지 않는다.
        String dedupKey = safeData.get(DEDUP_KEY);

        WebpushNotification.Builder webpushNotification = WebpushNotification.builder()
                .setTitle(title)
                .setBody(body)
                .setIcon("/catchmate-logo.svg");
        AndroidNotification.Builder androidNotification = AndroidNotification.builder();
        if (dedupKey != null) {
            webpushNotification.setTag(dedupKey);
            androidNotification.setTag(dedupKey);
        }

        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(webpushNotification.build())
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(androidNotification.build())
                        .build())
                .putAllData(safeData)
                .setToken(token)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.debug("FCM 메시지 전송 성공! Response ID: {}", response);
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
     * 계측만 하고 예외는 반드시 다시 던진다 — 여기서 삼키면 OutboxDispatcher 가 성공으로 오인해
     * Outbox 가 SUCCESS 로 확정되고 스케줄러 재시도 대상에서 영구 제외된다.
     */
    @Recover
    public void recover(RuntimeException e, Long userId, String token, String title, String body, Map<String, String> data) {
        // PermanentNotificationFailureException 은 noRetryFor 라 재시도되지 않지만 Spring Retry 는
        // non-retryable 예외도 이 복구 메서드로 넘긴다. 재시도 소진 지표와 섞이지 않게 그대로 전파한다.
        // (영구 실패 계측은 OutboxStateTransitioner 의 notification.outbox.failure{type=permanent} 담당)
        if (e instanceof PermanentNotificationFailureException) {
            throw e;
        }
        log.error("FCM 푸시 전송 최종 실패 (User: {}) - {}", userId, e.getMessage());
        meterRegistry.counter("notification.fcm.send.failure", "type", "retry_exhausted").increment();
        throw e;
    }
}
