package com.back.catchmate.application.chat.event;

import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.user.model.User;

import java.util.List;

/**
 * 채팅 메시지 FCM 알림 이벤트
 * 채팅방에 접속 중이지 않은 사용자들에게 Push 알림 전송
 */
public record ChatNotificationEvent(
        ChatMessage chatMessage,
        List<User> recipients,  // 알림을 받을 사용자들 (발신자 제외 채팅방 멤버)
        String title,
        String body
) {
    public static ChatNotificationEvent of(ChatMessage chatMessage, List<User> recipients) {
        String senderName = chatMessage.getSender().getNickName();
        String title = "새로운 채팅 메시지";
        String body = senderName + ": " + chatMessage.getContent();

        return new ChatNotificationEvent(chatMessage, recipients, title, body);
    }
}
