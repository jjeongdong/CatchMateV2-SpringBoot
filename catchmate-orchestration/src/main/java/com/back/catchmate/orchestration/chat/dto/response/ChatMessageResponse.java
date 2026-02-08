package com.back.catchmate.orchestration.chat.dto.response;

import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.chat.model.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {
    private Long messageId;
    private Long chatRoomId;
    private Long senderId;
    private String senderNickName;
    private String senderProfileImageUrl;
    private String content;
    private MessageType messageType;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .messageId(chatMessage.getId())
                .chatRoomId(chatMessage.getChatRoom().getId())
                .senderId(chatMessage.getSender().getId())
                .senderNickName(chatMessage.getSender().getNickName())
                .senderProfileImageUrl(chatMessage.getSender().getProfileImageUrl())
                .content(chatMessage.getContent())
                .messageType(chatMessage.getMessageType())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}
