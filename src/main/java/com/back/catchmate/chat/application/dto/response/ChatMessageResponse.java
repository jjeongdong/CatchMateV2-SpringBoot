package com.back.catchmate.chat.application.dto.response;

import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.user.domain.model.User;

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
    public static ChatMessageResponse from(ChatMessage chatMessage, User sender) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getChatRoomId(),
                chatMessage.getSenderId(),
                sender != null ? sender.getNickName() : null,
                sender != null ? sender.getProfileImageUrl() : null,
                chatMessage.getContent(),
                chatMessage.getMessageType(),
                chatMessage.getCreatedAt()
        );
    }
}
