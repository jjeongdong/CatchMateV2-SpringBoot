package com.back.catchmate.chat.application.dto;

import com.back.catchmate.chat.application.port.out.dto.ChatUserInfo;
import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
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

    public static ChatMessageCacheDto from(ChatMessage message, ChatUserInfo sender) {
        return new ChatMessageCacheDto(
                message.getId(),
                message.getChatRoomId(),
                message.getSenderId(),
                sender != null ? sender.nickName() : null,
                sender != null ? sender.profileImageUrl() : null,
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt()
        );
    }
}
