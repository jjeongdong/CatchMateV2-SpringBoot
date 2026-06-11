package com.back.catchmate.application.chat.dto;

import com.back.catchmate.chat.enums.MessageType;
import com.back.catchmate.domain.chat.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    public static ChatMessageCacheDto from(ChatMessage message) {
        return new ChatMessageCacheDto(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSender().getId(),
                message.getSender().getNickName(),
                message.getSender().getProfileImageUrl(),
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt()
        );
    }
}
