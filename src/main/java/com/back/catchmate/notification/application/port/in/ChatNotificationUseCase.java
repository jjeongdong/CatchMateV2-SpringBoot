package com.back.catchmate.notification.application.port.in;

public interface ChatNotificationUseCase {
    void saveOnChatMessageSent(Long chatRoomId, Long messageId, Long senderId, String content);

    void dispatchOnChatMessageSent(Long chatRoomId, Long messageId, Long senderId, String content);
}
