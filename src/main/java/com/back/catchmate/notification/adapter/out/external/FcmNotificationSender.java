package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.notification.application.port.out.dto.NotificationMessage;
import com.back.catchmate.notification.application.port.out.dto.NotificationSendResult;
import com.back.catchmate.notification.application.port.out.external.NotificationSenderPort;
import com.back.catchmate.notification.application.port.out.exception.PermanentNotificationFailureException;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
public class FcmNotificationSender implements NotificationSenderPort {
    // sendEach 1회당 상한(FCM 규격). 호출자의 배치 크기가 이보다 커도 안전하도록 여기서 잘라 보낸다.
    private static final int FCM_BATCH_LIMIT = 500;

    private final MeterRegistry meterRegistry;

    @Override
    public void sendNotification(Long userId, String token, String title, String body, Map<String, String> data) {
        Map<String, String> safeData = data != null ? data : Collections.emptyMap();

        log.debug("FCM 발송 시도 - User: {}, Token: {}, Title: {}, Body: {}, Data: {}", userId, token, title, body, safeData);

        try {
            String response = FirebaseMessaging.getInstance().send(toFcmMessage(token, title, body, safeData));
            log.debug("FCM 메시지 전송 성공! Response ID: {}", response);
        } catch (FirebaseMessagingException e) {
            if (isPermanentFailure(e)) {
                log.error("FCM 영구 실패 발생 - errorCode: {}, token: {}", e.getMessagingErrorCode(), token);
                meterRegistry.counter("notification.fcm.send.failure", "type", "permanent").increment();
                throw new PermanentNotificationFailureException(permanentFailureReason(e, token), e);
            }
            log.warn("FCM 전송 실패 (아웃박스가 재시도) - errorCode: {}, 에러 메시지: {}, 토큰: {}",
                    e.getMessagingErrorCode(), e.getMessage(), token);
            meterRegistry.counter("notification.fcm.send.failure", "type", "transient").increment();
            throw new BaseException(ErrorCode.FCM_SEND_FAILED);
        } catch (Exception e) {
            log.error("FCM 전송 중 예상치 못한 에러 발생 - Token: {}, Message: {}", token, e.getMessage(), e);
            meterRegistry.counter("notification.fcm.send.failure", "type", "unexpected").increment();
            throw e;
        }
    }

    @Override
    public List<NotificationSendResult> sendNotifications(List<NotificationMessage> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }

        List<NotificationSendResult> results = new ArrayList<>(messages.size());
        for (int start = 0; start < messages.size(); start += FCM_BATCH_LIMIT) {
            int end = Math.min(start + FCM_BATCH_LIMIT, messages.size());
            results.addAll(sendChunk(messages.subList(start, end)));
        }
        return results;
    }

    private List<NotificationSendResult> sendChunk(List<NotificationMessage> chunk) {
        List<Message> fcmMessages = chunk.stream()
                .map(message -> toFcmMessage(
                        message.token(),
                        message.title(),
                        message.body(),
                        message.data() != null ? message.data() : Collections.emptyMap()))
                .toList();

        BatchResponse batchResponse;
        try {
            // 건별 요청이 나가되 SDK 내부 스레드풀에서 동시에 발사된다(순차 발송 대비 병렬화가 이득).
            batchResponse = FirebaseMessaging.getInstance().sendEach(fcmMessages);
        } catch (FirebaseMessagingException e) {
            // 배치 호출 자체가 실패(인증·네트워크)해 건별 결과가 없다 → 전건을 재시도 대상으로 돌린다.
            log.error("FCM 배치 전송 실패 - count: {}, errorCode: {}", chunk.size(), e.getMessagingErrorCode(), e);
            meterRegistry.counter("notification.fcm.send.failure", "type", "batch_call").increment(chunk.size());
            String reason = "FCM 배치 호출 실패 - " + e.getMessage();
            return chunk.stream().map(message -> NotificationSendResult.ofRetryableFailure(reason)).toList();
        }

        log.debug("FCM 배치 전송 완료 - 요청 {}건, 성공 {}건, 실패 {}건",
                chunk.size(), batchResponse.getSuccessCount(), batchResponse.getFailureCount());

        List<SendResponse> responses = batchResponse.getResponses();
        List<NotificationSendResult> results = new ArrayList<>(responses.size());
        for (int i = 0; i < responses.size(); i++) {
            results.add(toSendResult(responses.get(i), chunk.get(i).token()));
        }
        return results;
    }

    private NotificationSendResult toSendResult(SendResponse response, String token) {
        if (response.isSuccessful()) {
            return NotificationSendResult.ofSuccess();
        }

        FirebaseMessagingException e = response.getException();
        if (e == null) {
            meterRegistry.counter("notification.fcm.send.failure", "type", "unexpected").increment();
            return NotificationSendResult.ofRetryableFailure("FCM 전송 실패 - 원인 불명");
        }
        if (isPermanentFailure(e)) {
            log.warn("FCM 영구 실패 - errorCode: {}, token: {}", e.getMessagingErrorCode(), token);
            meterRegistry.counter("notification.fcm.send.failure", "type", "permanent").increment();
            return NotificationSendResult.ofPermanentFailure(permanentFailureReason(e, token));
        }
        meterRegistry.counter("notification.fcm.send.failure", "type", "transient").increment();
        return NotificationSendResult.ofRetryableFailure(
                "FCM 전송 실패 - errorCode: " + e.getMessagingErrorCode() + ", message: " + e.getMessage());
    }

    private Message toFcmMessage(String token, String title, String body, Map<String, String> safeData) {
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

        return Message.builder()
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
    }

    private boolean isPermanentFailure(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == null) return false;
        return switch (code) {
            case UNREGISTERED, INVALID_ARGUMENT, SENDER_ID_MISMATCH -> true;
            default -> false;
        };
    }

    private String permanentFailureReason(FirebaseMessagingException e, String token) {
        return "FCM 영구 실패 - errorCode: " + e.getMessagingErrorCode() + ", token: " + token;
    }
}
