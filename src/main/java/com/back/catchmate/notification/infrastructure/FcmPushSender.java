package com.back.catchmate.notification.infrastructure;

import com.back.catchmate.notification.domain.NotificationPayload;
import com.back.catchmate.notification.domain.PushMessage;
import com.back.catchmate.notification.domain.PushOutcome;
import com.back.catchmate.notification.domain.PushSender;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * FCM 발송 어댑터.
 * <p>
 * <b>여기에 in-process 재시도(@Retryable)를 두지 않는다.</b> 재시도는 아웃박스가 담당한다
 * (retryCount + {@code NotificationScheduler} 주기 재발송 + PROCESSING 정체 회수).
 * 두 층을 겹치면 시도 횟수가 더해지는 게 아니라 곱해져서(3회 × maxRetryCount) FCM 장애 때 오히려 더 세게
 * 두드리고, backoff 대기 동안 발송 스레드가 묶인다. 배치 발송은 "N건 중 일부 실패"라 메서드 단위 재시도가
 * 성립하지도 않는다. 따라서 이 클래스는 <b>실패를 정확히 분류해서 알리는 것</b>까지만 책임진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmPushSender implements PushSender {
    // sendEach 1회당 상한(FCM 규격). 호출자의 배치 크기가 이보다 커도 안전하도록 여기서 잘라 보낸다.
    private static final int FCM_BATCH_LIMIT = 500;
    private static final String FAILURE_METRIC = "notification.fcm.send.failure";

    private final MeterRegistry meterRegistry;

    @Override
    public PushOutcome send(PushMessage message) {
        try {
            String response = FirebaseMessaging.getInstance().send(toFcmMessage(message));
            log.debug("FCM 전송 성공 userId={}, responseId={}", message.userId(), response);
            return PushOutcome.ofSuccess();
        } catch (FirebaseMessagingException e) {
            return toFailureResult(e);
        } catch (Exception e) {
            // Firebase 미초기화 같은 예상 밖 오류도 아웃박스 재시도에 맡긴다.
            log.error("FCM 전송 중 예상치 못한 오류 userId={}", message.userId(), e);
            countFailure("unexpected", 1);
            return PushOutcome.ofRetryableFailure("FCM 전송 중 예상치 못한 오류 - " + e.getMessage());
        }
    }

    @Override
    public List<PushOutcome> sendAll(List<PushMessage> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }
        List<PushOutcome> results = new ArrayList<>(messages.size());
        for (int start = 0; start < messages.size(); start += FCM_BATCH_LIMIT) {
            int end = Math.min(start + FCM_BATCH_LIMIT, messages.size());
            results.addAll(sendChunk(messages.subList(start, end)));
        }
        return results;
    }

    private List<PushOutcome> sendChunk(List<PushMessage> chunk) {
        List<Message> fcmMessages =
                chunk.stream().map(FcmPushSender::toFcmMessage).toList();

        BatchResponse batchResponse;
        try {
            // 건별 요청이 나가되 SDK 내부 스레드풀에서 동시에 발사된다(순차 발송 대비 병렬화가 이득).
            batchResponse = FirebaseMessaging.getInstance().sendEach(fcmMessages);
        } catch (FirebaseMessagingException e) {
            // 배치 호출 자체가 실패(인증·네트워크)해 건별 결과가 없다 → 전건을 재시도 대상으로 돌린다.
            log.error("FCM 배치 전송 실패 count={}, errorCode={}", chunk.size(), e.getMessagingErrorCode(), e);
            countFailure("batch_call", chunk.size());
            String reason = "FCM 배치 호출 실패 - " + e.getMessage();
            return chunk.stream()
                    .map(message -> PushOutcome.ofRetryableFailure(reason))
                    .toList();
        }

        log.debug(
                "FCM 배치 전송 완료 requested={}, succeeded={}, failed={}",
                chunk.size(),
                batchResponse.getSuccessCount(),
                batchResponse.getFailureCount());

        List<PushOutcome> results = new ArrayList<>(chunk.size());
        for (SendResponse response : batchResponse.getResponses()) {
            results.add(toResult(response));
        }
        return results;
    }

    private PushOutcome toResult(SendResponse response) {
        if (response.isSuccessful()) {
            return PushOutcome.ofSuccess();
        }
        FirebaseMessagingException e = response.getException();
        if (e == null) {
            countFailure("unexpected", 1);
            return PushOutcome.ofRetryableFailure("FCM 전송 실패 - 원인 불명");
        }
        return toFailureResult(e);
    }

    // 사유 문자열은 아웃박스 error_message 에 남으므로 토큰을 싣지 않는다.
    private PushOutcome toFailureResult(FirebaseMessagingException e) {
        if (isPermanentFailure(e)) {
            log.warn("FCM 영구 실패 errorCode={}", e.getMessagingErrorCode());
            countFailure("permanent", 1);
            return PushOutcome.ofPermanentFailure("FCM 영구 실패 - errorCode: " + e.getMessagingErrorCode());
        }
        countFailure("transient", 1);
        return PushOutcome.ofRetryableFailure(
                "FCM 전송 실패 - errorCode: " + e.getMessagingErrorCode() + ", message: " + e.getMessage());
    }

    private void countFailure(String type, int count) {
        meterRegistry.counter(FAILURE_METRIC, "type", type).increment(count);
    }

    private static Message toFcmMessage(PushMessage message) {
        Map<String, String> data = message.data() != null ? message.data() : Collections.emptyMap();
        // 같은 outbox 행의 재발송(FCM 타임아웃 재시도·PROCESSING 회수)이면 tag 가 같아서
        // OS 가 배너를 새로 쌓지 않고 기존 것을 교체한다. 수신 측이 아무 처리를 하지 않아도 중복이 보이지 않는다.
        String dedupKey = data.get(NotificationPayload.DEDUP_KEY);

        WebpushNotification.Builder webpushNotification = WebpushNotification.builder()
                .setTitle(message.title())
                .setBody(message.body())
                .setIcon("/catchmate-logo.svg");
        AndroidNotification.Builder androidNotification = AndroidNotification.builder();
        if (dedupKey != null) {
            webpushNotification.setTag(dedupKey);
            androidNotification.setTag(dedupKey);
        }

        return Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(webpushNotification.build())
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(androidNotification.build())
                        .build())
                .putAllData(data)
                .setToken(message.token())
                .build();
    }

    private static boolean isPermanentFailure(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == null) {
            return false;
        }
        return switch (code) {
            case UNREGISTERED, INVALID_ARGUMENT, SENDER_ID_MISMATCH -> true;
            default -> false;
        };
    }
}
