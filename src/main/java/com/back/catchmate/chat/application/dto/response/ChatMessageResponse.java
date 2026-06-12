package com.back.catchmate.chat.application.dto.response;

import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long chatRoomId,
        Long senderId,
        String senderNickName,
        String senderProfileImageUrl,
        String content,
        MessageType messageType,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getChatRoom().getId(),
                chatMessage.getSender().getId(),
                chatMessage.getSender().getNickName(),
                chatMessage.getSender().getProfileImageUrl(),
                chatMessage.getContent(),
                chatMessage.getMessageType(),
                chatMessage.getCreatedAt()
        );
    }
}
