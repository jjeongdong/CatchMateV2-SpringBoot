package com.back.catchmate.application.chat.port;

import com.back.catchmate.application.chat.event.ChatMessageEvent;
import com.back.catchmate.application.chat.event.ChatReadEvent;
import com.back.catchmate.application.notification.event.NotificationEvent;

public interface MessagePublisher {
    // 채팅 메시지 이벤트 발행 (message는 채팅 메시지에 대한 정보가 담긴 이벤트 객체)
    void publishChat(ChatMessageEvent chatMessageEvent);

    // FCM 알림 이벤트 발행 (userId는 알림을 받을 사용자 ID, data는 알림에 필요한 추가 정보)
    void publishNotification(NotificationEvent notificationEvent);

    // 채팅 읽음 이벤트 발행 (chatRoomId는 읽음 처리할 채팅방 ID, userId는 읽음 처리한 사용자 ID)
    void publishRead(ChatReadEvent chatReadEvent);
}
