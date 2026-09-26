package com.back.catchmate.chat.dto;

import com.back.catchmate.chat.entity.ChatMessage;
import com.back.catchmate.chat.entity.MessageType;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageCacheDto {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderNickname;
    private String senderProfileImageUrl;
    private String content;
    private MessageType messageType;
    private LocalDateTime createdAt;

    public static ChatMessageCacheDto from(ChatMessage message, UserInfo sender) {
        return new ChatMessageCacheDto(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSenderId(),
                sender != null ? sender.nickName() : null,
                sender != null ? sender.profileImageUrl() : null,
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt());
    }
}
