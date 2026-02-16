package com.back.catchmate.application.chat.port;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.application.notification.event.NotificationEvent;

public interface MessagePublisherPort {
    // 채팅 메시지 이벤트 발행
    void publishChat(ChatMessageEvent chatMessageEvent);

    // FCM 알림 이벤트 발행
    void publishNotification(NotificationEvent notificationEvent);
}
