package com.back.catchmate.chat.application.event;

import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.user.domain.model.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 이벤트 DTO
 * Redis Pub/Sub을 통해 전송되는 메시지 포맷
 */
@Builder
public record ChatMessageEvent(
        Long messageId,
        Long roomId,
        Long senderId,
        String senderNickname,
        String senderProfileImage,
        String content,
        MessageType messageType,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        LocalDateTime createdAt
) {
    public static ChatMessageEvent from(ChatMessage domain, User sender) {
        return ChatMessageEvent.builder()
                .messageId(domain.getId())
                .roomId(domain.getChatRoomId())
                .senderId(domain.getSenderId())
                .senderNickname(sender != null ? sender.getNickName() : null)
                .senderProfileImage(sender != null ? sender.getProfileImageUrl() : null)
                .content(domain.getContent())
                .messageType(domain.getMessageType())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
