package com.back.catchmate.chat.application.dto.result;

import com.back.catchmate.chat.domain.ChatHistoryPage;
import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.MessageType;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

// JSON 은 옛 ChatMessageResponse 와 같다.
public record ChatMessageResult(
        Long messageId,
        Long chatRoomId,
        Long senderId,
        String senderNickName,
        String senderProfileImageUrl,
        String content,
        MessageType messageType,
        LocalDateTime createdAt) {

    public static ChatMessageResult of(ChatMessage message, UserInfo sender) {
        return new ChatMessageResult(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSenderId(),
                sender != null ? sender.nickName() : null,
                sender != null ? sender.profileImageUrl() : null,
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt());
    }

    public static ChatMessageResult from(ChatHistoryPage.Entry entry) {
        return new ChatMessageResult(
                entry.id(),
                entry.roomId(),
                entry.senderId(),
                entry.senderNickname(),
                entry.senderProfileImageUrl(),
                entry.content(),
                entry.messageType(),
                entry.createdAt());
    }
}
