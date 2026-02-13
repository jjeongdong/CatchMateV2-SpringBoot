package com.back.catchmate.application.chat.port;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.application.chat.event.ChatReadEvent;
import com.back.catchmate.application.notification.event.NotificationEvent;

public interface MessagePublisher {
    // 채팅 메시지 이벤트 발행
    void publishChat(ChatMessageEvent chatMessageEvent);

    // FCM 알림 이벤트 발행
    void publishNotification(NotificationEvent notificationEvent);

    // 채팅 읽음 이벤트 발행
    void publishRead(ChatReadEvent chatReadEvent);
}
