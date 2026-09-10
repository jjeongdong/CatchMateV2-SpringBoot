package com.back.catchmate.chat.dto.command;

import com.back.catchmate.chat.entity.MessageType;

public record ChatMessageCommand(
        Long chatRoomId,
        Long senderId,
        String content,
        MessageType messageType
) {
    public ChatMessageCommand(Long chatRoomId, Long senderId, String content, MessageType messageType) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
    }
}
