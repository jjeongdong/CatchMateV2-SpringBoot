package com.back.catchmate.infrastructure.notification.sender;

import com.back.catchmate.domain.notification.port.NotificationSenderPort;
import com.back.catchmate.domain.user.port.UserOnlineStatusPort;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmNotificationSender implements NotificationSenderPort {
    private final UserOnlineStatusPort userOnlineStatusPort;

    /**
     * [재시도 진입점]
     * 외부(Listener)에서 호출하는 메서드입니다.
     * 여기서 예외가 발생하면 설정된 backoff 시간만큼 대기 후 재시도합니다.
     */
    @Override
    @Retryable(
            retryFor = {RuntimeException.class}, // 모든 런타임 예외(FCM 연결 실패 등)에 대해 재시도
            maxAttempts = 3,                     // 최대 3회 시도 (최초 1회 + 재시도 2회)
            backoff = @Backoff(delay = 1000)     // 재시도 간격 1초
    )
    public void sendNotificationIfOffline(Long userId, String token, String title, String body, Map<String, String> data) {
        // 1. 온라인 여부 체크 (Redis 등)
        if (userOnlineStatusPort.isUserOnline(userId)) {
            log.debug("User {} is ONLINE - FCM 알림 전송 생략", userId);
            return;
        }

        // 2. 실제 전송 로직 호출
        log.info("User {} is OFFLINE - FCM 알림 전송 시도", userId);
        sendNotification(token, title, body, data);
    }

    /**
     * [실제 전송 로직]
     * 재시도를 위해 예외를 swallow하지 않고 throw 하는 것이 핵심입니다.
     */
    @Override
    public void sendNotification(String token, String title, String body, Map<String, String> data) {
        Map<String, String> safeData = data != null ? data : Collections.emptyMap();

        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(safeData)
                .setToken(token)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 메시지 전송 성공: {}", response);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM 전송 실패 (재시도 예정) - 토큰: {}, 에러: {}", token, e.getMessage());
            throw new RuntimeException("FCM 전송 실패", e);
        }
    }

    @Override
    public void sendMulticastNotification(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) return;
        Map<String, String> safeData = data != null ? data : Collections.emptyMap();

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(safeData)
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            if (response.getFailureCount() > 0) {
                log.warn("FCM 멀티캐스트 일부 실패: {}건", response.getFailureCount());
            }
        } catch (FirebaseMessagingException e) {
            log.error("FCM 멀티캐스트 치명적 오류", e);
            throw new RuntimeException("FCM 멀티캐스트 실패", e);
        }
    }

    /**
     * [복구 메서드]
     * 3번의 재시도(Retry)가 모두 실패했을 때 실행됩니다.
     */
    @Recover
    public void recover(RuntimeException e, Long userId, String token, String title, String body, Map<String, String> data) {
        log.error("FCM 푸시 전송 최종 실패 (User: {}) - {}", userId, e.getMessage());
        // failedPushRepository.save(...);
    }
}
