package com.back.catchmate.chat.domain.event;

import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.MessageType;
import java.time.LocalDateTime;

/**
 * 커밋된 메시지를 모든 서버의 구독자에게 퍼뜨리는 이벤트. Redis Pub/Sub 으로 나가 그대로 STOMP 로 전달된다.
 * 필드 이름은 서버 간 JSON 계약이다 — 롤링 배포 중 옛 서버와 새 서버가 섞여도 통해야 하므로 바꾸지 않는다.
 * 날짜 형식(yyyy-MM-dd HH:mm:ss)은 domain 이 Jackson 을 모르도록 infrastructure 의 Pub/Sub 직렬화 설정이 맡는다.
 */
public record ChatMessageBroadcastEvent(
        Long messageId,
        Long roomId,
        Long senderId,
        String senderNickname,
        String senderProfileImage,
        String content,
        MessageType messageType,
        LocalDateTime createdAt) {

    public static ChatMessageBroadcastEvent of(ChatMessage message, String senderNickname, String senderProfileImage) {
        return new ChatMessageBroadcastEvent(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSenderId(),
                senderNickname,
                senderProfileImage,
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt());
    }
}
