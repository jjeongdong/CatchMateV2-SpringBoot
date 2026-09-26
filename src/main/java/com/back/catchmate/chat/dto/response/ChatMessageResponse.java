package com.back.catchmate.chat.dto.response;

import com.back.catchmate.chat.entity.ChatMessage;
import com.back.catchmate.chat.entity.MessageType;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long chatRoomId,
        Long senderId,
        String senderNickName,
        String senderProfileImageUrl,
        String content,
        MessageType messageType,
        LocalDateTime createdAt) {
    public static ChatMessageResponse from(ChatMessage chatMessage, UserInfo sender) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getChatRoom().getId(),
                chatMessage.getSenderId(),
                sender != null ? sender.nickName() : null,
                sender != null ? sender.profileImageUrl() : null,
                chatMessage.getContent(),
                chatMessage.getMessageType(),
                chatMessage.getCreatedAt());
    }
}
