package com.back.catchmate.notification.application.port.in;

public interface ChatNotificationDispatchUseCase {
    void dispatchOnChatMessageSent(Long chatRoomId, Long messageId, Long senderId, String content);
}
