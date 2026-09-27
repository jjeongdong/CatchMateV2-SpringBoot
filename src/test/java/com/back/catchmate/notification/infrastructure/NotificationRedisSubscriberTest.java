package com.back.catchmate.notification.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationRedisSubscriberTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationRedisSubscriber subscriber;

    @BeforeEach
    void setUp() {
        subscriber = new NotificationRedisSubscriber(
                Jackson2ObjectMapperBuilder.json().build(), messagingTemplate);
    }

    @Test
    @DisplayName("한 메시지의 수신자 전원에게 개인 큐로 분배한다")
    void distributesToEachUser() {
        subscriber.onNotification("{\"userIds\":[1,2],\"data\":{\"type\":\"CHAT\"}}");

        then(messagingTemplate).should().convertAndSendToUser("1", "/queue/notifications", Map.of("type", "CHAT"));
        then(messagingTemplate).should().convertAndSendToUser("2", "/queue/notifications", Map.of("type", "CHAT"));
    }

    @Test
    @DisplayName("깨진 메시지는 로그만 남기고 삼킨다 (리스너 스레드를 죽이지 않는다)")
    void swallowsBrokenMessage() {
        subscriber.onNotification("not-json");

        then(messagingTemplate).should(never()).convertAndSendToUser(anyString(), anyString(), any());
    }
}
