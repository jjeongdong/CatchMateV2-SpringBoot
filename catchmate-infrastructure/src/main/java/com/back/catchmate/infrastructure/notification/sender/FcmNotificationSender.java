package com.back.catchmate.infrastructure.notification.sender;

import com.back.catchmate.domain.notification.port.NotificationSender;
import com.back.catchmate.domain.user.port.UserOnlineStatusPort;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmNotificationSender implements NotificationSender {

    private final UserOnlineStatusPort userOnlineStatusPort;
    @Override
    public void sendNotification(String token, String title, String body, Map<String, String> data) {
        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .setToken(token)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 메시지 전송 성공: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 메시지 전송 실패 (토큰: {}): {}", token, e.getMessage());
        }
    }

    @Override
    public void sendMulticastNotification(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) return;

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        log.warn("FCM 전송 실패 토큰: {}", tokens.get(i));
                    }
                }
            }
            log.info("FCM 멀티캐스트 전송 완료: 성공 {}건, 실패 {}건", response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("FCM 멀티캐스트 전송 중 심각한 오류 발생", e);
        }
    }

    @Override
    public void sendNotificationIfOffline(Long userId, String token, String title, String body, Map<String, String> data) {
        // 사용자가 온라인 상태인지 확인
        if (userOnlineStatusPort.isUserOnline(userId)) {
            log.debug("User {} is ONLINE - FCM 알림 전송 생략", userId);
            return;
        }

        // 오프라인 상태인 경우에만 FCM 알림 전송
        log.info("User {} is OFFLINE - FCM 알림 전송", userId);
        sendNotification(token, title, body, data);
    }
}
