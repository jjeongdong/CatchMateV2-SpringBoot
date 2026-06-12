package com.back.catchmate.chat.application.event;

import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.notification.domain.model.NotificationTemplate;
import com.back.catchmate.user.domain.model.User;

import java.util.List;

/**
 * 채팅 메시지 FCM 알림 이벤트
 * 채팅방에 접속 중이지 않은 사용자들에게 Push 알림 전송
 */
public record ChatNotificationEvent(
        ChatMessage chatMessage,
        User sender,
        List<User> recipients,  // 알림을 받을 사용자들 (발신자 제외 채팅방 멤버)
        String title,
        String body
) {
    public static ChatNotificationEvent of(ChatMessage chatMessage, User sender, List<User> recipients) {
        String senderName = sender.getNickName();
        String content = chatMessage.getContent();

        String title = NotificationTemplate.CHAT_NEW_MESSAGE.formatTitle(senderName);
        String body = NotificationTemplate.CHAT_NEW_MESSAGE.formatBody(content);

        return new ChatNotificationEvent(chatMessage, sender, recipients, title, body);
    }
}
